package com.pruebafullstack.cti.model;

import java.time.Instant;

public record AgentState(
        String agentId,
        AgentStatus status,
        Instant updatedAt
) {
}
