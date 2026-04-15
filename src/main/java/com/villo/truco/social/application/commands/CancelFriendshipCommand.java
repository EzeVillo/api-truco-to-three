package com.villo.truco.social.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record CancelFriendshipCommand(String username, PlayerId actorId) {

  public CancelFriendshipCommand {

    Objects.requireNonNull(username);
    Objects.requireNonNull(actorId);
  }

  public CancelFriendshipCommand(final String username, final String actorId) {

    this(username, PlayerId.of(actorId));
  }

}
