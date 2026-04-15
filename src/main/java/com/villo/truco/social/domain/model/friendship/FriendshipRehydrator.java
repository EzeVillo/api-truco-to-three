package com.villo.truco.social.domain.model.friendship;

public final class FriendshipRehydrator {

  private FriendshipRehydrator() {

  }

  public static Friendship rehydrate(final FriendshipSnapshot snapshot) {

    return Friendship.reconstruct(snapshot.id(), snapshot.requesterId(), snapshot.addresseeId(),
        snapshot.status());
  }

}
