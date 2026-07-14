package com.pruebafullstack.cti.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pruebafullstack.cti.dto.CtiEventDto;
import com.pruebafullstack.cti.dto.CtiSnapshotDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CtiEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(CtiEventProcessor.class);

    private final ObjectMapper objectMapper;
    private final CtiStateService stateService;
    private final EventStreamService eventStreamService;

    public CtiEventProcessor(
            ObjectMapper objectMapper,
            CtiStateService stateService,
            EventStreamService eventStreamService
    ) {
        this.objectMapper = objectMapper;
        this.stateService = stateService;
        this.eventStreamService = eventStreamService;
    }

    public void processRawMessage(String payload) {
        try {
            CtiEventDto event = objectMapper.readValue(payload, CtiEventDto.class);
            log.debug("Evento CTI recibido: {}", event);

            stateService.applyEvent(event)
                    .ifPresent(this::broadcastSnapshot);
        } catch (JsonProcessingException ex) {
            log.warn("Mensaje CTI inválido, no es JSON esperado: {}", payload, ex);
        } catch (RuntimeException ex) {
            log.error("Error inesperado procesando evento CTI: {}", payload, ex);
        }
    }

    private void broadcastSnapshot(CtiSnapshotDto snapshot) {
        eventStreamService.broadcast(snapshot);
    }
}
