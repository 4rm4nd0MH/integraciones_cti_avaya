package com.pruebafullstack.cti.service;

import com.pruebafullstack.cti.dto.AgentDto;
import com.pruebafullstack.cti.dto.CallDto;
import com.pruebafullstack.cti.dto.CtiEventDto;
import com.pruebafullstack.cti.dto.CtiSnapshotDto;
import com.pruebafullstack.cti.dto.ExtensionDto;
import com.pruebafullstack.cti.model.AgentState;
import com.pruebafullstack.cti.model.AgentStatus;
import com.pruebafullstack.cti.model.CallState;
import com.pruebafullstack.cti.model.CallStatus;
import com.pruebafullstack.cti.model.CtiEventType;
import com.pruebafullstack.cti.model.ExtensionState;
import com.pruebafullstack.cti.model.ExtensionStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CtiStateService {

    private static final Logger log = LoggerFactory.getLogger(CtiStateService.class);

    private final ConcurrentHashMap<String, CallState> activeCalls = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AgentState> agents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ExtensionState> extensions = new ConcurrentHashMap<>();
    private final Set<String> processedEventFingerprints = ConcurrentHashMap.newKeySet();

    private volatile boolean ctiConnected;
    private volatile Instant lastHeartbeatAt;

    public synchronized Optional<CtiSnapshotDto> applyEvent(CtiEventDto event) {
        Optional<CtiEventType> eventType = parseEventType(event.eventType());
        if (eventType.isEmpty()) {
            log.warn("Evento CTI descartado por tipo desconocido: {}", event.eventType());
            return Optional.empty();
        }

        if (eventType.get() == CtiEventType.HEARTBEAT) {
            lastHeartbeatAt = eventTime(event);
            log.debug("Heartbeat CTI recibido en {}", lastHeartbeatAt);
            return Optional.of(snapshot());
        }

        if (eventType.get() == CtiEventType.CTI_CONNECTED) {
            ctiConnected = true;
            log.info("Evento administrativo CTI recibido: conexión activa");
            return Optional.of(snapshot());
        }

        if (eventType.get() == CtiEventType.CTI_DISCONNECTED) {
            ctiConnected = false;
            log.warn("Evento administrativo CTI recibido: conexión inactiva");
            return Optional.of(snapshot());
        }

        if (!hasRequiredCallData(event)) {
            log.warn("Evento CTI descartado por datos incompletos: {}", event);
            return Optional.empty();
        }

        String fingerprint = fingerprint(event);
        if (!processedEventFingerprints.add(fingerprint)) {
            log.debug("Evento CTI duplicado ignorado: {}", fingerprint);
            return Optional.empty();
        }

        applyCallTransition(eventType.get(), event);
        refreshDerivedStates();
        CtiSnapshotDto snapshot = snapshot();
        log.info("Evento CTI procesado: type={}, callId={}, activeCalls={}",
                eventType.get(), event.callId(), snapshot.activeCalls().size());
        return Optional.of(snapshot);
    }

    public synchronized List<CallDto> getActiveCalls() {
        return snapshotCalls();
    }

    public synchronized List<AgentDto> getAgents() {
        return snapshotAgents();
    }

    public synchronized List<ExtensionDto> getExtensions() {
        return snapshotExtensions();
    }

    public synchronized CtiSnapshotDto snapshot() {
        return new CtiSnapshotDto(
                snapshotCalls(),
                snapshotAgents(),
                snapshotExtensions(),
                ctiConnected,
                lastHeartbeatAt,
                Instant.now()
        );
    }

    public void markCtiConnected() {
        ctiConnected = true;
    }

    public void markCtiDisconnected() {
        ctiConnected = false;
    }

    private void applyCallTransition(CtiEventType eventType, CtiEventDto event) {
        Instant now = eventTime(event);
        switch (eventType) {
            case CALL_RECEIVED -> upsertCall(event, CallStatus.RINGING, now);
            case CALL_ANSWERED -> upsertCall(event, CallStatus.IN_CALL, now);
            case CALL_HOLD -> upsertCall(event, CallStatus.ON_HOLD, now);
            case CALL_RESUME -> upsertCall(event, CallStatus.IN_CALL, now);
            case CALL_TRANSFER -> upsertCall(event, CallStatus.TRANSFERRING, now);
            case CALL_ENDED -> activeCalls.remove(event.callId());
            case CTI_CONNECTED, CTI_DISCONNECTED, HEARTBEAT -> {
                // Estos eventos administrativos se manejan antes de validar datos de llamada.
            }
        }
    }

    private void upsertCall(CtiEventDto event, CallStatus status, Instant now) {
        activeCalls.compute(event.callId(), (callId, current) -> {
            Instant startedAt = current != null ? current.startedAt() : now;
            return new CallState(
                    callId,
                    event.extension(),
                    event.agentId(),
                    event.phoneNumber(),
                    status,
                    startedAt,
                    now
            );
        });
    }

    private void refreshDerivedStates() {
        ConcurrentHashMap<String, AgentState> nextAgents = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, ExtensionState> nextExtensions = new ConcurrentHashMap<>();
        Instant now = Instant.now();

        for (CallState call : activeCalls.values()) {
            if (StringUtils.hasText(call.agentId())) {
                nextAgents.merge(
                        call.agentId(),
                        new AgentState(call.agentId(), agentStatusFor(call.status()), now),
                        this::mergeAgentPriority
                );
            }

            if (StringUtils.hasText(call.extension())) {
                nextExtensions.merge(
                        call.extension(),
                        new ExtensionState(call.extension(), extensionStatusFor(call.status()), now),
                        this::mergeExtensionPriority
                );
            }
        }

        agents.replaceAll((agentId, current) ->
                nextAgents.getOrDefault(agentId, new AgentState(agentId, AgentStatus.AVAILABLE, now)));
        extensions.replaceAll((extension, current) ->
                nextExtensions.getOrDefault(extension, new ExtensionState(extension, ExtensionStatus.IDLE, now)));
        agents.putAll(nextAgents);
        extensions.putAll(nextExtensions);
    }

    private AgentState mergeAgentPriority(AgentState current, AgentState candidate) {
        return agentPriority(candidate.status()) > agentPriority(current.status()) ? candidate : current;
    }

    private ExtensionState mergeExtensionPriority(ExtensionState current, ExtensionState candidate) {
        return extensionPriority(candidate.status()) > extensionPriority(current.status()) ? candidate : current;
    }

    private AgentStatus agentStatusFor(CallStatus callStatus) {
        return callStatus == CallStatus.ON_HOLD ? AgentStatus.ON_HOLD : AgentStatus.BUSY;
    }

    private ExtensionStatus extensionStatusFor(CallStatus callStatus) {
        return switch (callStatus) {
            case RINGING -> ExtensionStatus.RINGING;
            case ON_HOLD -> ExtensionStatus.ON_HOLD;
            case IN_CALL, TRANSFERRING -> ExtensionStatus.BUSY;
        };
    }

    private int agentPriority(AgentStatus status) {
        return switch (status) {
            case AVAILABLE -> 0;
            case ON_HOLD -> 1;
            case BUSY -> 2;
        };
    }

    private int extensionPriority(ExtensionStatus status) {
        return switch (status) {
            case IDLE -> 0;
            case RINGING -> 1;
            case ON_HOLD -> 2;
            case BUSY -> 3;
        };
    }

    private List<CallDto> snapshotCalls() {
        return activeCalls.values().stream()
                .map(call -> new CallDto(
                        call.callId(),
                        call.extension(),
                        call.agentId(),
                        call.phoneNumber(),
                        call.status(),
                        call.startedAt(),
                        call.updatedAt()
                ))
                .sorted(Comparator.comparing(CallDto::updatedAt).reversed())
                .toList();
    }

    private List<AgentDto> snapshotAgents() {
        return agents.values().stream()
                .map(agent -> new AgentDto(agent.agentId(), agent.status(), agent.updatedAt()))
                .sorted(Comparator.comparing(AgentDto::agentId))
                .toList();
    }

    private List<ExtensionDto> snapshotExtensions() {
        return extensions.values().stream()
                .map(extension -> new ExtensionDto(extension.extension(), extension.status(), extension.updatedAt()))
                .sorted(Comparator.comparing(ExtensionDto::extension))
                .toList();
    }

    private Optional<CtiEventType> parseEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return Optional.empty();
        }

        try {
            return Optional.of(CtiEventType.valueOf(eventType.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private boolean hasRequiredCallData(CtiEventDto event) {
        return StringUtils.hasText(event.callId())
                && StringUtils.hasText(event.extension())
                && StringUtils.hasText(event.agentId());
    }

    private String fingerprint(CtiEventDto event) {
        List<String> parts = new ArrayList<>();
        parts.add(nullSafe(event.eventType()));
        parts.add(nullSafe(event.callId()));
        parts.add(nullSafe(event.extension()));
        parts.add(nullSafe(event.agentId()));
        parts.add(nullSafe(event.phoneNumber()));
        parts.add(String.valueOf(event.timestamp()));
        return String.join("|", parts);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private Instant eventTime(CtiEventDto event) {
        return event.timestamp() != null ? event.timestamp() : Instant.now();
    }
}
