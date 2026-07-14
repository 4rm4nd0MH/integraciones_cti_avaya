package com.pruebafullstack.cti.dto;

import com.pruebafullstack.cti.model.AgentStatus;
import java.time.Instant;

public record AgentDto(
        String agentId,
        AgentStatus status,
        Instant updatedAt
) {
}
