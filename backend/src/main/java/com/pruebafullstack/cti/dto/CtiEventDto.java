package com.pruebafullstack.cti.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CtiEventDto(
        String eventType,
        String callId,
        String extension,
        String agentId,
        String phoneNumber,
        Instant timestamp
) {
}
