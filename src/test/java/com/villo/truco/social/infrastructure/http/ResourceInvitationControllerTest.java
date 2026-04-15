package com.villo.truco.social.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.social.application.dto.ResourceInvitationDTO;
import com.villo.truco.social.application.ports.in.AcceptResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.CancelResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.CreateResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.DeclineResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.GetResourceInvitationsUseCase;
import com.villo.truco.social.application.ports.in.GetSentResourceInvitationsUseCase;
import com.villo.truco.social.infrastructure.http.dto.request.CreateResourceInvitationRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("ResourceInvitationController")
class ResourceInvitationControllerTest {

  @Test
  @DisplayName("expone endpoints de invitaciones con codigos y payload esperados")
  void flows() {

    final var invitationId = "44444444-4444-4444-4444-444444444444";
    final var actorId = "11111111-1111-1111-1111-111111111111";
    final var targetId = "55555555-5555-5555-5555-555555555555";
    final CreateResourceInvitationUseCase createResourceInvitationUseCase = mock(
        CreateResourceInvitationUseCase.class);
    final AcceptResourceInvitationUseCase acceptResourceInvitationUseCase = mock(
        AcceptResourceInvitationUseCase.class);
    final DeclineResourceInvitationUseCase declineResourceInvitationUseCase = mock(
        DeclineResourceInvitationUseCase.class);
    final CancelResourceInvitationUseCase cancelResourceInvitationUseCase = mock(
        CancelResourceInvitationUseCase.class);
    final GetResourceInvitationsUseCase getResourceInvitationsUseCase = mock(
        GetResourceInvitationsUseCase.class);
    final GetSentResourceInvitationsUseCase getSentResourceInvitationsUseCase = mock(
        GetSentResourceInvitationsUseCase.class);
    when(createResourceInvitationUseCase.handle(any())).thenReturn(
        new ResourceInvitationDTO(invitationId, "juancho", "martina", "MATCH", targetId, "PENDING",
            Instant.parse("2026-04-04T12:10:00Z").toEpochMilli()));
    when(acceptResourceInvitationUseCase.handle(any())).thenReturn(
        new ResourceInvitationDTO(invitationId, "juancho", "martina", "MATCH", targetId, "ACCEPTED",
            Instant.parse("2026-04-04T12:10:00Z").toEpochMilli()));
    when(declineResourceInvitationUseCase.handle(any())).thenReturn(
        new ResourceInvitationDTO(invitationId, "juancho", "martina", "MATCH", targetId, "DECLINED",
            Instant.parse("2026-04-04T12:10:00Z").toEpochMilli()));
    when(getResourceInvitationsUseCase.handle(any())).thenReturn(List.of(
        new ResourceInvitationDTO(invitationId, "juancho", "martina", "MATCH", targetId, "PENDING",
            Instant.parse("2026-04-04T12:10:00Z").toEpochMilli())));
    when(cancelResourceInvitationUseCase.handle(any())).thenReturn(
        new ResourceInvitationDTO(invitationId, "juancho", "martina", "MATCH", targetId,
            "CANCELLED", Instant.parse("2026-04-04T12:10:00Z").toEpochMilli()));
    when(getSentResourceInvitationsUseCase.handle(any())).thenReturn(List.of(
        new ResourceInvitationDTO(invitationId, "juancho", "martina", "MATCH", targetId, "PENDING",
            Instant.parse("2026-04-04T12:10:00Z").toEpochMilli())));

    final var controller = new ResourceInvitationController(createResourceInvitationUseCase,
        acceptResourceInvitationUseCase, declineResourceInvitationUseCase,
        cancelResourceInvitationUseCase, getResourceInvitationsUseCase,
        getSentResourceInvitationsUseCase);
    final var jwt = Jwt.withTokenValue("token").header("alg", "none").subject(actorId).build();

    assertThat(controller.createInvitation(
            new CreateResourceInvitationRequest("martina", "MATCH", targetId), jwt)
        .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.acceptInvitation(invitationId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.declineInvitation(invitationId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.getInvitations(jwt).getBody()).hasSize(1);
    assertThat(controller.cancelInvitation(invitationId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.getSentInvitations(jwt).getBody()).hasSize(1);
  }

}
