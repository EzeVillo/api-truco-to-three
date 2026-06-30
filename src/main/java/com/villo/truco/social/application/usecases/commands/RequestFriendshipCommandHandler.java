package com.villo.truco.social.application.usecases.commands;

import com.villo.truco.social.application.commands.RequestFriendshipCommand;
import com.villo.truco.social.application.exceptions.FriendshipAlreadyExistsException;
import com.villo.truco.social.application.exceptions.FriendshipRequestAlreadyPendingException;
import com.villo.truco.social.application.ports.in.RequestFriendshipUseCase;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.FriendshipRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import com.villo.truco.social.domain.ports.SocialPreferencesRepository;
import java.util.Objects;

public final class RequestFriendshipCommandHandler implements RequestFriendshipUseCase {

  private final SocialUserGuard socialUserGuard;
  private final FriendshipQueryRepository friendshipQueryRepository;
  private final FriendshipRepository friendshipRepository;
  private final SocialPreferencesRepository socialPreferencesRepository;
  private final SocialEventNotifier socialEventNotifier;

  public RequestFriendshipCommandHandler(final SocialUserGuard socialUserGuard,
      final FriendshipQueryRepository friendshipQueryRepository,
      final FriendshipRepository friendshipRepository,
      final SocialPreferencesRepository socialPreferencesRepository,
      final SocialEventNotifier socialEventNotifier) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.friendshipRepository = Objects.requireNonNull(friendshipRepository);
    this.socialPreferencesRepository = Objects.requireNonNull(socialPreferencesRepository);
    this.socialEventNotifier = Objects.requireNonNull(socialEventNotifier);
  }

  @Override
  public Void handle(final RequestFriendshipCommand command) {

    this.socialUserGuard.ensureRegisteredUser(command.requesterId());
    final var addresseeId = this.socialUserGuard.findRegisteredUserIdByUsername(command.username());

    this.friendshipQueryRepository.findPendingByPlayers(command.requesterId(), addresseeId)
        .ifPresent(existing -> {
          throw new FriendshipRequestAlreadyPendingException();
        });
    this.friendshipQueryRepository.findAcceptedByPlayers(command.requesterId(), addresseeId)
        .ifPresent(existing -> {
          throw new FriendshipAlreadyExistsException();
        });

    final var addresseeAcceptsRequests = this.socialPreferencesRepository.findByPlayerId(
        addresseeId).map(SocialPreferences::acceptsFriendRequests).orElse(true);

    final var friendship = Friendship.request(command.requesterId(), addresseeId,
        addresseeAcceptsRequests);
    this.friendshipRepository.save(friendship);
    this.socialEventNotifier.publishDomainEvents(friendship.getFriendshipDomainEvents());
    friendship.clearDomainEvents();

    return null;
  }

}
