package com.villo.truco.social.application.usecases.commands;

import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.social.application.commands.CreateResourceInvitationCommand;
import com.villo.truco.social.application.dto.ResourceInvitationDTO;
import com.villo.truco.social.application.exceptions.FriendshipRequiredException;
import com.villo.truco.social.application.ports.in.CreateResourceInvitationUseCase;
import com.villo.truco.social.application.services.InvitationTargetService;
import com.villo.truco.social.application.services.ResourceInvitationPolicy;
import com.villo.truco.social.application.services.SocialInvitationExpirationPolicy;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.time.Clock;
import java.util.Objects;

public final class CreateResourceInvitationCommandHandler implements
    CreateResourceInvitationUseCase {

  private final SocialUserGuard socialUserGuard;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final FriendshipQueryRepository friendshipQueryRepository;
  private final ResourceInvitationPolicy resourceInvitationPolicy;
  private final ResourceInvitationRepository resourceInvitationRepository;
  private final SocialEventNotifier socialEventNotifier;
  private final InvitationTargetService invitationTargetService;
  private final SocialInvitationExpirationPolicy socialInvitationExpirationPolicy;
  private final SocialViewAssembler socialViewAssembler;
  private final Clock clock;

  public CreateResourceInvitationCommandHandler(final SocialUserGuard socialUserGuard,
      final PlayerAvailabilityChecker playerAvailabilityChecker,
      final FriendshipQueryRepository friendshipQueryRepository,
      final ResourceInvitationPolicy resourceInvitationPolicy,
      final ResourceInvitationRepository resourceInvitationRepository,
      final SocialEventNotifier socialEventNotifier,
      final InvitationTargetService invitationTargetService,
      final SocialInvitationExpirationPolicy socialInvitationExpirationPolicy,
      final SocialViewAssembler socialViewAssembler, final Clock clock) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.resourceInvitationPolicy = Objects.requireNonNull(resourceInvitationPolicy);
    this.resourceInvitationRepository = Objects.requireNonNull(resourceInvitationRepository);
    this.socialEventNotifier = Objects.requireNonNull(socialEventNotifier);
    this.invitationTargetService = Objects.requireNonNull(invitationTargetService);
    this.socialInvitationExpirationPolicy = Objects.requireNonNull(
        socialInvitationExpirationPolicy);
    this.socialViewAssembler = Objects.requireNonNull(socialViewAssembler);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public ResourceInvitationDTO handle(final CreateResourceInvitationCommand command) {

    this.socialUserGuard.ensureRegisteredUser(command.senderId());
    final var recipientId = this.socialUserGuard.findRegisteredUserIdByUsername(
        command.recipientUsername());

    if (!this.friendshipQueryRepository.existsAcceptedByPlayers(command.senderId(), recipientId)) {
      throw new FriendshipRequiredException();
    }

    this.playerAvailabilityChecker.ensureAvailable(recipientId);

    this.resourceInvitationPolicy.ensureNoDuplicatePending(command.senderId(), recipientId,
        command.targetType(), command.targetId());

    this.invitationTargetService.ensureInvitableForSending(command.senderId(), command.targetType(),
        command.targetId());
    final var invitation = ResourceInvitation.create(command.senderId(), recipientId,
        command.targetType(), command.targetId(), this.clock.instant(),
        this.socialInvitationExpirationPolicy.expirationFor(command.targetType()));

    this.resourceInvitationRepository.save(invitation);
    this.socialEventNotifier.publishDomainEvents(invitation.getResourceInvitationDomainEvents());
    invitation.clearDomainEvents();

    return this.socialViewAssembler.toInvitationDto(invitation);
  }

}
