package com.villo.truco.social.application.services;

import com.villo.truco.social.application.exceptions.FriendshipNotFoundException;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.util.Objects;

public final class FriendshipResolver {

  private final FriendshipQueryRepository friendshipQueryRepository;

  public FriendshipResolver(final FriendshipQueryRepository friendshipQueryRepository) {

    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
  }

  public Friendship resolve(final FriendshipId friendshipId) {

    return this.friendshipQueryRepository.findById(friendshipId)
        .orElseThrow(() -> new FriendshipNotFoundException(friendshipId));
  }

}
