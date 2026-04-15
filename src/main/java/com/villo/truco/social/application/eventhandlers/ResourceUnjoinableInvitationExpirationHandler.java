package com.villo.truco.social.application.eventhandlers;

import com.villo.truco.application.events.ResourceBecameUnjoinable;
import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResourceUnjoinableInvitationExpirationHandler implements
    ApplicationEventHandler<ResourceBecameUnjoinable> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ResourceUnjoinableInvitationExpirationHandler.class);

  private final ResourceInvitationQueryRepository resourceInvitationQueryRepository;
  private final ResourceInvitationRepository resourceInvitationRepository;
  private final SocialEventNotifier socialEventNotifier;
  private final TransactionalRunner transactionalRunner;

  public ResourceUnjoinableInvitationExpirationHandler(
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository,
      final ResourceInvitationRepository resourceInvitationRepository,
      final SocialEventNotifier socialEventNotifier,
      final TransactionalRunner transactionalRunner) {

    this.resourceInvitationQueryRepository = Objects.requireNonNull(
        resourceInvitationQueryRepository);
    this.resourceInvitationRepository = Objects.requireNonNull(resourceInvitationRepository);
    this.socialEventNotifier = Objects.requireNonNull(socialEventNotifier);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
  }

  @Override
  public Class<ResourceBecameUnjoinable> eventType() {

    return ResourceBecameUnjoinable.class;
  }

  @Override
  public void handle(final ResourceBecameUnjoinable event) {

    final var targetType = ResourceInvitationTargetType.valueOf(event.targetType());
    final var pending = this.resourceInvitationQueryRepository.findPendingByTarget(targetType,
        event.targetId());

    for (final var invitation : pending) {
      try {
        this.transactionalRunner.run(() -> this.expire(invitation.getId().value().toString()));
      } catch (final RuntimeException ex) {
        LOGGER.error(
            "Failed to expire invitation for unjoinable resource: invitationId={}, targetType={}, targetId={}",
            invitation.getId().value(), event.targetType(), event.targetId(), ex);
      }
    }
  }

  private void expire(final String invitationId) {

    final var invitation = this.resourceInvitationQueryRepository.findById(
        ResourceInvitationId.of(invitationId)).orElse(null);
    if (invitation == null) {
      return;
    }

    invitation.expire();

    if (!invitation.getResourceInvitationDomainEvents().isEmpty()) {
      this.resourceInvitationRepository.save(invitation);
      this.socialEventNotifier.publishDomainEvents(invitation.getResourceInvitationDomainEvents());
      invitation.clearDomainEvents();
    }
  }

}
