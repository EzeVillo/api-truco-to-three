package com.villo.truco.social.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.ResourceInvitationDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationAcceptedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationCancelledEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDeclinedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDomainEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationExpiredEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationReceivedEvent;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ResourceInvitationTimeoutEventHandler")
class ResourceInvitationTimeoutEventHandlerTest {

  private final ResourceInvitationId invitationId = ResourceInvitationId.generate();
  private final PlayerId sender = PlayerId.generate();
  private final PlayerId recipient = PlayerId.generate();
  private final ResourceInvitationTargetType targetType = ResourceInvitationTargetType.MATCH;
  private final String targetId = UUID.randomUUID().toString();
  private final Instant expiresAt = Instant.now().plusSeconds(600);
  private TimeoutScheduler timeoutScheduler;
  private ResourceInvitationDomainEventHandler<ResourceInvitationDomainEvent> handler;

  @BeforeEach
  void setUp() {

    timeoutScheduler = mock(TimeoutScheduler.class);
    final var dispatcher = new TimeoutActionDispatcher();
    dispatcher.register(EntityType.RESOURCE_INVITATION, id -> () -> {
    });

    handler = new ResourceInvitationTimeoutEventHandler(timeoutScheduler, dispatcher);
  }

  @Test
  @DisplayName("Debe programar timeout al recibir invitación con expiresAt del evento")
  void scheduleTimeoutOnInvitationReceived() {

    final var event = new ResourceInvitationReceivedEvent(invitationId, sender, recipient,
        targetType, targetId, expiresAt);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.RESOURCE_INVITATION,
        invitationId.value().toString());
    verify(timeoutScheduler).schedule(eq(expectedKey), eq(expiresAt), any(Runnable.class));
  }

  @Test
  @DisplayName("Debe cancelar timeout cuando la invitación es aceptada")
  void cancelTimeoutOnInvitationAccepted() {

    final var event = new ResourceInvitationAcceptedEvent(invitationId, sender, recipient,
        targetType, targetId, expiresAt);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.RESOURCE_INVITATION,
        invitationId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout cuando la invitación es rechazada")
  void cancelTimeoutOnInvitationDeclined() {

    final var event = new ResourceInvitationDeclinedEvent(invitationId, sender, recipient,
        targetType, targetId, expiresAt);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.RESOURCE_INVITATION,
        invitationId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout cuando la invitación es cancelada por el remitente")
  void cancelTimeoutOnInvitationCancelled() {

    final var event = new ResourceInvitationCancelledEvent(invitationId, sender, recipient,
        targetType, targetId, expiresAt);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.RESOURCE_INVITATION,
        invitationId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout cuando la invitación expira")
  void cancelTimeoutOnInvitationExpired() {

    final var event = new ResourceInvitationExpiredEvent(invitationId, sender, recipient,
        targetType, targetId, expiresAt);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.RESOURCE_INVITATION,
        invitationId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Devuelve ResourceInvitationDomainEvent como tipo de evento escuchado")
  void handlerAcceptsResourceInvitationDomainEventClass() {

    assertThat(handler.eventType()).isEqualTo(ResourceInvitationDomainEvent.class);
  }

}
