package com.villo.truco.social.application.services;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.ports.FriendshipSpectateEligibilityResolver;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.util.Objects;

public final class SocialFriendshipSpectateEligibilitySupport implements
    FriendshipSpectateEligibilityResolver {

  private final FriendshipQueryRepository friendshipQueryRepository;

  public SocialFriendshipSpectateEligibilitySupport(
      final FriendshipQueryRepository friendshipQueryRepository) {

    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
  }

  @Override
  public boolean canSpectateAsFriend(final Match match, final PlayerId spectatorId) {

    Objects.requireNonNull(match);
    Objects.requireNonNull(spectatorId);

    return this.isAcceptedFriend(spectatorId, match.getPlayerOne()) || (match.getPlayerTwo() != null
        && this.isAcceptedFriend(spectatorId, match.getPlayerTwo()));
  }

  private boolean isAcceptedFriend(final PlayerId spectatorId, final PlayerId playerId) {

    return !spectatorId.equals(playerId) && this.friendshipQueryRepository.existsAcceptedByPlayers(
        spectatorId, playerId);
  }

}
