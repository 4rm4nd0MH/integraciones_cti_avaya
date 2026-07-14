package com.pruebafullstack.cti.model;

import java.time.Instant;

public record ExtensionState(
        String extension,
        ExtensionStatus status,
        Instant updatedAt
) {
}
