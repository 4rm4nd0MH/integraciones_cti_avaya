package com.pruebafullstack.cti.service;

import com.pruebafullstack.cti.dto.CtiSnapshotDto;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class EventStreamService {

    private static final Logger log = LoggerFactory.getLogger(EventStreamService.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final CtiStateService stateService;

    public EventStreamService(CtiStateService stateService) {
        this.stateService = stateService;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));

        send(emitter, stateService.snapshot());
        return emitter;
    }

    public void broadcast(CtiSnapshotDto snapshot) {
        for (SseEmitter emitter : emitters) {
            send(emitter, snapshot);
        }
    }

    private void send(SseEmitter emitter, CtiSnapshotDto snapshot) {
        try {
            emitter.send(SseEmitter.event()
                    .name("cti-snapshot")
                    .data(snapshot));
        } catch (IOException | IllegalStateException ex) {
            emitters.remove(emitter);
            log.debug("Cliente SSE removido por error de envío: {}", ex.getMessage());
        }
    }
}
