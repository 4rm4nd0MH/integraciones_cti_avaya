package com.pruebafullstack.cti.controller;

import com.pruebafullstack.cti.dto.AgentDto;
import com.pruebafullstack.cti.dto.CallDto;
import com.pruebafullstack.cti.dto.CtiSnapshotDto;
import com.pruebafullstack.cti.dto.ExtensionDto;
import com.pruebafullstack.cti.service.CtiStateService;
import com.pruebafullstack.cti.service.EventStreamService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class CtiQueryController {

    private final CtiStateService stateService;
    private final EventStreamService eventStreamService;

    public CtiQueryController(CtiStateService stateService, EventStreamService eventStreamService) {
        this.stateService = stateService;
        this.eventStreamService = eventStreamService;
    }

    @GetMapping("/calls/active")
    public List<CallDto> activeCalls() {
        return stateService.getActiveCalls();
    }

    @GetMapping("/agents")
    public List<AgentDto> agents() {
        return stateService.getAgents();
    }

    @GetMapping("/extensions")
    public List<ExtensionDto> extensions() {
        return stateService.getExtensions();
    }

    @GetMapping("/cti/snapshot")
    public CtiSnapshotDto snapshot() {
        return stateService.snapshot();
    }

    @GetMapping(value = "/cti/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return eventStreamService.subscribe();
    }
}
