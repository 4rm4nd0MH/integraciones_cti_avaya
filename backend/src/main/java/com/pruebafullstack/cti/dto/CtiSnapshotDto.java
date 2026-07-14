package com.pruebafullstack.cti.dto;

import java.time.Instant;
import java.util.List;

public record CtiSnapshotDto(
        List<CallDto> activeCalls,
        List<AgentDto> agents,
        List<ExtensionDto> extensions,
        boolean ctiConnected,
        Instant lastHeartbeatAt,
        Instant generatedAt
) {
}
