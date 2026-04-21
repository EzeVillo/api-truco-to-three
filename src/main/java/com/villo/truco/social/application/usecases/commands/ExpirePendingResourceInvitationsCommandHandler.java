package com.villo.truco.social.application.usecases.commands;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.social.application.ports.in.ExpirePendingResourceInvitationsUseCase;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.time.Clock;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExpirePendingResourceInvitationsCommandHandler implements
    ExpirePendingResourceInvitationsUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ExpirePendingResourceInvitationsCommandHandler.class);

  private final ResourceInvitationQueryRepository resourceInvitationQueryRepository;
  private final ResourceInvitationRepository resourceInvitationRepository;
  private final SocialEventNotifier socialEventNotifier;
  private final TransactionalRunner transactionalRunner;
  private final Clock clock;

  public ExpirePendingResourceInvitationsCommandHandler(
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository,
      final ResourceInvitationRepository resourceInvitationRepository,
      final SocialEventNotifier socialEventNotifier, final TransactionalRunner transactionalRunner,
      final Clock clock) {

    this.resourceInvitationQueryRepository = Objects.requireNonNull(
        resourceInvitationQueryRepository);
    this.resourceInvitationRepository = Objects.requireNonNull(resourceInvitationRepository);
    this.socialEventNotifier = Objects.requireNonNull(socialEventNotifier);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Void handle(final Void unused) {

    for (final var invitation : this.resourceInvitationQueryRepository.findPendingInvitations()) {
      try {
        this.transactionalRunner.run(() -> this.process(invitation.getId().value().toString()));
      } catch (final RuntimeException ex) {
        LOGGER.error("Failed to process social invitation expiration: invitationId={}",
            invitation.getId().value(), ex);
      }
    }
    return null;
  }

  private void process(final String invitationId) {

    final var invitation = this.resourceInvitationQueryRepository.findById(
        ResourceInvitationId.of(invitationId)).orElse(null);
    if (invitation == null) {
      return;
    }

    if (!invitation.expireIfNeeded(this.clock.instant())) {
      return;
    }

    this.resourceInvitationRepository.save(invitation);
    this.socialEventNotifier.publishDomainEvents(invitation.getResourceInvitationDomainEvents());
    invitation.clearDomainEvents();
  }

}
