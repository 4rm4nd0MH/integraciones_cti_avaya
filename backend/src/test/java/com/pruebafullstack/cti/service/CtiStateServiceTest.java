package com.pruebafullstack.cti.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pruebafullstack.cti.dto.CtiEventDto;
import com.pruebafullstack.cti.model.CallStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CtiStateServiceTest {

    private final CtiStateService stateService = new CtiStateService();

    @Test
    void shouldCreateActiveCallWhenCallIsReceived() {
        CtiEventDto event = new CtiEventDto(
                "CALL_RECEIVED",
                "CALL-1001",
                "101",
                "A-100",
                "5551234567",
                Instant.parse("2026-08-12T10:00:00Z")
        );

        stateService.applyEvent(event);

        assertThat(stateService.getActiveCalls()).hasSize(1);
        assertThat(stateService.getActiveCalls().get(0).status()).isEqualTo(CallStatus.RINGING);
        assertThat(stateService.getAgents().get(0).status().name()).isEqualTo("BUSY");
        assertThat(stateService.getExtensions().get(0).status().name()).isEqualTo("RINGING");
    }

    @Test
    void shouldHandleAdministrativeCtiConnectionEvent() {
        CtiEventDto event = new CtiEventDto(
                "CTI_CONNECTED",
                null,
                null,
                null,
                null,
                Instant.parse("2026-08-12T10:00:00Z")
        );

        assertThat(stateService.applyEvent(event)).isPresent();
        assertThat(stateService.snapshot().ctiConnected()).isTrue();
    }

    @Test
    void shouldIgnoreDuplicatedEvents() {
        CtiEventDto event = new CtiEventDto(
                "CALL_ANSWERED",
                "CALL-1001",
                "101",
                "A-100",
                "5551234567",
                Instant.parse("2026-08-12T10:00:01Z")
        );

        assertThat(stateService.applyEvent(event)).isPresent();
        assertThat(stateService.applyEvent(event)).isEmpty();
        assertThat(stateService.getActiveCalls()).hasSize(1);
        assertThat(stateService.getActiveCalls().get(0).status()).isEqualTo(CallStatus.IN_CALL);
    }

    @Test
    void shouldRemoveActiveCallWhenCallEnds() {
        stateService.applyEvent(new CtiEventDto(
                "CALL_RECEIVED",
                "CALL-1001",
                "101",
                "A-100",
                "5551234567",
                Instant.parse("2026-08-12T10:00:00Z")
        ));

        stateService.applyEvent(new CtiEventDto(
                "CALL_ENDED",
                "CALL-1001",
                "101",
                "A-100",
                "5551234567",
                Instant.parse("2026-08-12T10:00:30Z")
        ));

        assertThat(stateService.getActiveCalls()).isEmpty();
        assertThat(stateService.getAgents().get(0).status().name()).isEqualTo("AVAILABLE");
        assertThat(stateService.getExtensions().get(0).status().name()).isEqualTo("IDLE");
    }
}
