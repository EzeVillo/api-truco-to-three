package com.villo.truco.social.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record AcceptFriendshipCommand(String username, PlayerId actorId) {

  public AcceptFriendshipCommand {

    Objects.requireNonNull(username);
    Objects.requireNonNull(actorId);
  }

  public AcceptFriendshipCommand(final String username, final String actorId) {

    this(username, PlayerId.of(actorId));
  }

}
