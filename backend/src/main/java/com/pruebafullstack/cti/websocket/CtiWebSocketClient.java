package com.pruebafullstack.cti.websocket;

import com.pruebafullstack.cti.config.CtiProperties;
import com.pruebafullstack.cti.service.CtiEventProcessor;
import com.pruebafullstack.cti.service.CtiStateService;
import com.pruebafullstack.cti.service.EventStreamService;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class CtiWebSocketClient extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CtiWebSocketClient.class);

    private final CtiProperties properties;
    private final StandardWebSocketClient webSocketClient;
    private final ThreadPoolTaskScheduler ctiTaskScheduler;
    private final CtiEventProcessor eventProcessor;
    private final CtiStateService stateService;
    private final EventStreamService eventStreamService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    private volatile WebSocketSession session;
    private volatile Duration nextReconnectDelay;

    public CtiWebSocketClient(
            CtiProperties properties,
            StandardWebSocketClient webSocketClient,
            ThreadPoolTaskScheduler ctiTaskScheduler,
            CtiEventProcessor eventProcessor,
            CtiStateService stateService,
            EventStreamService eventStreamService
    ) {
        this.properties = properties;
        this.webSocketClient = webSocketClient;
        this.ctiTaskScheduler = ctiTaskScheduler;
        this.eventProcessor = eventProcessor;
        this.stateService = stateService;
        this.eventStreamService = eventStreamService;
        this.nextReconnectDelay = properties.getReconnectInitialDelay();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isEnabled()) {
            log.info("Cliente CTI deshabilitado por configuración");
            return;
        }

        if (!StringUtils.hasText(properties.getWebsocketUrl())) {
            log.warn("Cliente CTI no iniciado: falta configurar cti.websocket-url");
            return;
        }

        running.set(true);
        connect();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        this.connecting.set(false);
        this.nextReconnectDelay = properties.getReconnectInitialDelay();
        markConnected();
        log.info("Conectado al Mock CTI Server: {}", properties.getWebsocketUrl());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        eventProcessor.processRawMessage(message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Error de transporte CTI: {}", exception.getMessage(), exception);
        markDisconnected();
        scheduleReconnect();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        this.session = null;
        this.connecting.set(false);
        markDisconnected();
        log.warn("Conexión CTI cerrada: code={}, reason={}", status.getCode(), status.getReason());
        scheduleReconnect();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        closeCurrentSession();
    }

    private void connect() {
        if (!running.get() || !connecting.compareAndSet(false, true)) {
            return;
        }

        URI uri = URI.create(properties.getWebsocketUrl());
        log.info("Intentando conexión CTI a {}", uri);

        webSocketClient.execute(this, new WebSocketHttpHeaders(), uri)
                .whenComplete((webSocketSession, error) -> {
                    connecting.set(false);
                    if (error != null) {
                        markDisconnected();
                        log.warn("No fue posible conectar al Mock CTI: {}", error.getMessage());
                        scheduleReconnect();
                    }
                });
    }

    private void scheduleReconnect() {
        if (!running.get()) {
            return;
        }

        Duration delay = nextReconnectDelay;
        nextReconnectDelay = delay.multipliedBy(2);
        if (nextReconnectDelay.compareTo(properties.getReconnectMaxDelay()) > 0) {
            nextReconnectDelay = properties.getReconnectMaxDelay();
        }

        log.info("Reintentando conexión CTI en {} ms", delay.toMillis());
        ctiTaskScheduler.schedule(this::connect, Instant.now().plus(delay));
    }

    private void closeCurrentSession() {
        WebSocketSession currentSession = session;
        if (currentSession == null || !currentSession.isOpen()) {
            return;
        }

        try {
            currentSession.close(CloseStatus.NORMAL);
        } catch (Exception ex) {
            log.debug("No se pudo cerrar sesión CTI limpiamente: {}", ex.getMessage());
        }
    }

    private void markConnected() {
        stateService.markCtiConnected();
        eventStreamService.broadcast(stateService.snapshot());
    }

    private void markDisconnected() {
        stateService.markCtiDisconnected();
        eventStreamService.broadcast(stateService.snapshot());
    }
}
