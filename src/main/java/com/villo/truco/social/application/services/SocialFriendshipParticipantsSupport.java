package com.villo.truco.social.application.services;

import com.villo.truco.application.ports.FriendshipParticipantsPort;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SocialFriendshipParticipantsSupport implements FriendshipParticipantsPort {

  private final FriendshipQueryRepository friendshipQueryRepository;

  public SocialFriendshipParticipantsSupport(
      final FriendshipQueryRepository friendshipQueryRepository) {

    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
  }

  @Override
  public Optional<List<PlayerId>> findParticipantsIfAccepted(final String friendshipId,
      final PlayerId requesterId) {

    return this.friendshipQueryRepository.findById(FriendshipId.of(friendshipId))
        .filter(Friendship::isAccepted).filter(f -> f.involves(requesterId))
        .map(f -> List.of(f.getRequesterId(), f.getAddresseeId()));
  }

}
