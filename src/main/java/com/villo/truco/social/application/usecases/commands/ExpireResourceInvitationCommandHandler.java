package com.villo.truco.social.application.usecases.commands;

import com.villo.truco.application.ports.RetryableTransactionalRunner;
import com.villo.truco.social.application.ports.in.ExpireResourceInvitationUseCase;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.time.Clock;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExpireResourceInvitationCommandHandler implements
    ExpireResourceInvitationUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ExpireResourceInvitationCommandHandler.class);

  private final ResourceInvitationQueryRepository queryRepository;
  private final ResourceInvitationRepository repository;
  private final SocialEventNotifier eventNotifier;
  private final RetryableTransactionalRunner transactionalRunner;
  private final Clock clock;

  public ExpireResourceInvitationCommandHandler(
      final ResourceInvitationQueryRepository queryRepository,
      final ResourceInvitationRepository repository, final SocialEventNotifier eventNotifier,
      final RetryableTransactionalRunner transactionalRunner, final Clock clock) {

    this.queryRepository = Objects.requireNonNull(queryRepository);
    this.repository = Objects.requireNonNull(repository);
    this.eventNotifier = Objects.requireNonNull(eventNotifier);
    this.transactionalRunner = Objects.requireNonNull(transactionalRunner);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Void handle(final ResourceInvitationId id) {

    try {
      this.transactionalRunner.run(() -> this.processExpiration(id));
    } catch (final Exception e) {
      LOGGER.error("Failed to expire resource invitation: invitationId={}", id, e);
    }
    return null;
  }

  private void processExpiration(final ResourceInvitationId id) {

    final var invitation = this.queryRepository.findById(id).orElse(null);
    if (invitation == null) {
      return;
    }
    if (!invitation.expireIfNeeded(this.clock.instant())) {
      return;
    }
    this.repository.save(invitation);
    this.eventNotifier.publishDomainEvents(invitation.getResourceInvitationDomainEvents());
    invitation.clearDomainEvents();
  }

}
