package com.villo.truco.social.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record UpdateSocialPreferencesCommand(PlayerId playerId, boolean acceptsFriendRequests) {

  public UpdateSocialPreferencesCommand {

    Objects.requireNonNull(playerId);
  }

  public UpdateSocialPreferencesCommand(final String playerId,
      final boolean acceptsFriendRequests) {

    this(PlayerId.of(playerId), acceptsFriendRequests);
  }

}
