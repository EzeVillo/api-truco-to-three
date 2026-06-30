package com.villo.truco.social.domain.model.preferences;

import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class SocialPreferences extends AggregateBase<PlayerId> {

  private boolean acceptsFriendRequests;

  private SocialPreferences(final PlayerId playerId, final boolean acceptsFriendRequests) {

    super(Objects.requireNonNull(playerId));
    this.acceptsFriendRequests = acceptsFriendRequests;
  }

  public static SocialPreferences create(final PlayerId playerId) {

    return new SocialPreferences(playerId, true);
  }

  public static SocialPreferences reconstruct(final PlayerId playerId,
      final boolean acceptsFriendRequests) {

    return new SocialPreferences(playerId, acceptsFriendRequests);
  }

  public void changeAcceptsFriendRequests(final boolean acceptsFriendRequests) {

    this.acceptsFriendRequests = acceptsFriendRequests;
  }

  public boolean acceptsFriendRequests() {

    return this.acceptsFriendRequests;
  }

  public SocialPreferencesSnapshot snapshot() {

    return new SocialPreferencesSnapshot(this.getId(), this.acceptsFriendRequests);
  }

}
