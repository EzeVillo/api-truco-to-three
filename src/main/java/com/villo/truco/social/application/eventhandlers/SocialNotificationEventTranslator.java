package com.villo.truco.social.application.eventhandlers;

import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.events.FriendAvailabilityNotification;
import com.villo.truco.social.application.events.SocialEventNotification;
import com.villo.truco.social.application.services.FriendAvailabilityResolver;
import com.villo.truco.social.domain.model.friendship.events.SocialDomainEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationAcceptedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationCancelledEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDeclinedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDomainEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationExpiredEvent;
import java.util.List;
import java.util.Objects;

public final class SocialNotificationEventTranslator implements
    DomainEventHandler<SocialDomainEvent> {

  static final String AVAILABILITY_CHANGED_EVENT_TYPE = "FRIEND_AVAILABILITY_CHANGED";

  private final SocialEventMapper socialEventMapper;
  private final FriendAvailabilityResolver friendAvailabilityResolver;
  private final ApplicationEventPublisher applicationEventPublisher;

  public SocialNotificationEventTranslator(final SocialEventMapper socialEventMapper,
      final FriendAvailabilityResolver friendAvailabilityResolver,
      final ApplicationEventPublisher applicationEventPublisher) {

    this.socialEventMapper = Objects.requireNonNull(socialEventMapper);
    this.friendAvailabilityResolver = Objects.requireNonNull(friendAvailabilityResolver);
    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
  }

  private static boolean transitionsOutOfPending(final ResourceInvitationDomainEvent event) {

    return event instanceof ResourceInvitationAcceptedEvent
        || event instanceof ResourceInvitationCancelledEvent
        || event instanceof ResourceInvitationDeclinedEvent
        || event instanceof ResourceInvitationExpiredEvent;
  }

  @Override
  public Class<SocialDomainEvent> eventType() {

    return SocialDomainEvent.class;
  }

  @Override
  public void handle(final SocialDomainEvent event) {

    this.applicationEventPublisher.publish(
        new SocialEventNotification(event.getRecipients(), event.getEventType(),
            event.getTimestamp(), this.socialEventMapper.map(event)));

    this.emitAvailabilityDeltas(event);
  }

  /**
   * Cuando una invitacion deja de estar pendiente (aceptada, cancelada, rechazada o expirada), el
   * pendiente bloqueante entre las partes desaparece, por lo que se recalcula y emite un delta de
   * disponibilidad hacia cada parte respecto de la otra (T036). El push se difiere a post-commit
   * via {@link FriendAvailabilityNotification}.
   */
  private void emitAvailabilityDeltas(final SocialDomainEvent event) {

    if (!(event instanceof ResourceInvitationDomainEvent invitation) || !transitionsOutOfPending(
        invitation)) {
      return;
    }

    final var sender = invitation.getSenderId();
    final var recipient = invitation.getRecipientId();
    this.publishAvailabilityDelta(sender, recipient, invitation.getTimestamp());
    this.publishAvailabilityDelta(recipient, sender, invitation.getTimestamp());
  }

  private void publishAvailabilityDelta(final PlayerId viewerId, final PlayerId friendId,
      final long timestamp) {

    this.friendAvailabilityResolver.resolveFriendDeltaFor(viewerId, friendId).ifPresent(
        availability -> this.applicationEventPublisher.publish(
            new FriendAvailabilityNotification(List.of(viewerId), AVAILABILITY_CHANGED_EVENT_TYPE,
                timestamp, availability.toPayload())));
  }

}
