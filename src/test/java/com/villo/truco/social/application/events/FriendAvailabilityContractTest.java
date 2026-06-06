package com.villo.truco.social.application.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.social.application.dto.FriendAvailabilityDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityStatus;
import com.villo.truco.social.application.dto.FriendBusyReason;
import com.villo.truco.social.application.dto.SpectatableMatchRefDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Contrato de payload social de disponibilidad")
class FriendAvailabilityContractTest {

  @Test
  @DisplayName("el payload solo expone los campos publicos del contrato y nada privado del match")
  void payloadOnlyExposesPublicContractFields() {

    final var ref = new SpectatableMatchRefDTO("550e8400-e29b-41d4-a716-446655440000",
        "IN_PROGRESS");
    final var payload = new FriendAvailabilityDTO("martina", true, FriendAvailabilityStatus.BUSY,
        FriendBusyReason.IN_MATCH, ref).toPayload();

    assertThat(payload).containsOnlyKeys("friendUsername", "online", "availability", "busyReason",
        "spectatableMatch");
    assertThat(payload).containsEntry("friendUsername", "martina");
    assertThat(payload).containsEntry("online", true);
  }

  @Test
  @DisplayName("la referencia espectable solo lleva id y status, sin estado privado de ronda")
  void spectatableRefOnlyCarriesIdAndStatus() {

    final var ref = new SpectatableMatchRefDTO("match-1", "IN_PROGRESS");

    assertThat(ref.id()).isEqualTo("match-1");
    assertThat(ref.status()).isEqualTo("IN_PROGRESS");
    assertThat(SpectatableMatchRefDTO.class.getRecordComponents()).extracting(
        java.lang.reflect.RecordComponent::getName).containsExactly("id", "status");
  }

  @Test
  @DisplayName("un amigo disponible no puede tener busyReason")
  void availableFriendHasNoBusyReason() {

    final var dto = new FriendAvailabilityDTO("agus", false, FriendAvailabilityStatus.AVAILABLE,
        null, null);

    assertThat(dto.busyReason()).isNull();
    assertThat(dto.toPayload()).containsEntry("busyReason", null);
  }

}
