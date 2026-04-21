package com.villo.truco.social.application.usecases.commands;

import com.villo.truco.social.application.commands.DeclineFriendshipCommand;
import com.villo.truco.social.application.exceptions.FriendshipNotFoundException;
import com.villo.truco.social.application.ports.in.DeclineFriendshipUseCase;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.FriendshipRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.util.Objects;

public final class DeclineFriendshipCommandHandler implements DeclineFriendshipUseCase {

  private final SocialUserGuard socialUserGuard;
  private final FriendshipQueryRepository friendshipQueryRepository;
  private final FriendshipRepository friendshipRepository;
  private final SocialEventNotifier socialEventNotifier;

  public DeclineFriendshipCommandHandler(final SocialUserGuard socialUserGuard,
      final FriendshipQueryRepository friendshipQueryRepository,
      final FriendshipRepository friendshipRepository,
      final SocialEventNotifier socialEventNotifier) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.friendshipRepository = Objects.requireNonNull(friendshipRepository);
    this.socialEventNotifier = Objects.requireNonNull(socialEventNotifier);
  }

  @Override
  public Void handle(final DeclineFriendshipCommand command) {

    this.socialUserGuard.ensureRegisteredUser(command.actorId());
    final var requesterId = this.socialUserGuard.findRegisteredUserIdByUsername(command.username());

    final var friendship = this.friendshipQueryRepository.findPendingByRequesterAndAddressee(
        requesterId, command.actorId()).orElseThrow(
        () -> FriendshipNotFoundException.pendingRequestFromUsername(command.username()));
    friendship.decline(command.actorId());

    this.friendshipRepository.save(friendship);
    this.socialEventNotifier.publishDomainEvents(friendship.getFriendshipDomainEvents());
    friendship.clearDomainEvents();

    return null;
  }

}
