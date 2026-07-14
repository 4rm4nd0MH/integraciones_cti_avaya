package com.pruebafullstack.cti.dto;

import com.pruebafullstack.cti.model.CallStatus;
import java.time.Instant;

public record CallDto(
        String callId,
        String extension,
        String agentId,
        String phoneNumber,
        CallStatus status,
        Instant startedAt,
        Instant updatedAt
) {
}
