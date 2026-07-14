package com.pruebafullstack.cti.dto;

import com.pruebafullstack.cti.model.ExtensionStatus;
import java.time.Instant;

public record ExtensionDto(
        String extension,
        ExtensionStatus status,
        Instant updatedAt
) {
}
