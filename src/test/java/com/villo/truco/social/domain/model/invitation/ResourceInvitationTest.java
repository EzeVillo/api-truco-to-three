package com.villo.truco.social.domain.model.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationAcceptedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationCancelledEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationReceivedEvent;
import com.villo.truco.social.domain.model.invitation.exceptions.OnlyRecipientCanRespondResourceInvitationException;
import com.villo.truco.social.domain.model.invitation.exceptions.OnlySenderCanCancelResourceInvitationException;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ResourceInvitation")
class ResourceInvitationTest {

  @Test
  @DisplayName("crea invitacion pendiente y emite evento recibido")
  void createPendingInvitation() {

    final var invitation = ResourceInvitation.create(PlayerId.generate(), PlayerId.generate(),
        ResourceInvitationTargetType.MATCH, "11111111-1111-1111-1111-111111111111",
        Instant.parse("2026-04-04T12:00:00Z"), Duration.ofMinutes(10));

    assertThat(invitation.getStatus()).isEqualTo(ResourceInvitationStatus.PENDING);
    assertThat(invitation.getResourceInvitationDomainEvents()).singleElement()
        .isInstanceOf(ResourceInvitationReceivedEvent.class);
  }

  @Test
  @DisplayName("acepta la invitacion solo el destinatario antes de expirar")
  void recipientAcceptsInvitation() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var invitation = ResourceInvitation.create(sender, recipient,
        ResourceInvitationTargetType.LEAGUE, "11111111-1111-1111-1111-111111111111",
        Instant.parse("2026-04-04T12:00:00Z"), Duration.ofMinutes(10));
    invitation.clearDomainEvents();

    invitation.accept(recipient);

    assertThat(invitation.getStatus()).isEqualTo(ResourceInvitationStatus.ACCEPTED);
    assertThat(invitation.getResourceInvitationDomainEvents()).singleElement()
        .isInstanceOf(ResourceInvitationAcceptedEvent.class);
  }

  @Test
  @DisplayName("permite cancelar al remitente y emite evento cancelado para el destinatario")
  void senderCancelsInvitation() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var invitation = ResourceInvitation.create(sender, recipient,
        ResourceInvitationTargetType.CUP, "11111111-1111-1111-1111-111111111111",
        Instant.parse("2026-04-04T12:00:00Z"), Duration.ofMinutes(10));
    invitation.clearDomainEvents();

    invitation.cancel(sender);

    assertThat(invitation.getStatus()).isEqualTo(ResourceInvitationStatus.CANCELLED);
    assertThat(invitation.getResourceInvitationDomainEvents()).singleElement()
        .isInstanceOfSatisfying(ResourceInvitationCancelledEvent.class, event -> {
          assertThat(event.getRecipients()).containsExactly(recipient);
          assertThat(event.getStatus()).isEqualTo(ResourceInvitationStatus.CANCELLED);
        });
  }

  @Test
  @DisplayName("impide responder a quien no es destinatario")
  void rejectsNonRecipientResponse() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var invitation = ResourceInvitation.create(sender, recipient,
        ResourceInvitationTargetType.MATCH, "11111111-1111-1111-1111-111111111111",
        Instant.parse("2026-04-04T12:00:00Z"), Duration.ofMinutes(10));

    assertThatThrownBy(() -> invitation.accept(sender)).isInstanceOf(
        OnlyRecipientCanRespondResourceInvitationException.class);
  }

  @Test
  @DisplayName("impide cancelar a quien no es remitente")
  void rejectsNonSenderCancellation() {

    final var sender = PlayerId.generate();
    final var recipient = PlayerId.generate();
    final var invitation = ResourceInvitation.create(sender, recipient,
        ResourceInvitationTargetType.MATCH, "11111111-1111-1111-1111-111111111111",
        Instant.parse("2026-04-04T12:00:00Z"), Duration.ofMinutes(10));

    assertThatThrownBy(() -> invitation.cancel(recipient)).isInstanceOf(
        OnlySenderCanCancelResourceInvitationException.class);
  }

}
