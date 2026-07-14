package com.pruebafullstack.cti.model;

import java.time.Instant;

public record CallState(
        String callId,
        String extension,
        String agentId,
        String phoneNumber,
        CallStatus status,
        Instant startedAt,
        Instant updatedAt
) {
}
