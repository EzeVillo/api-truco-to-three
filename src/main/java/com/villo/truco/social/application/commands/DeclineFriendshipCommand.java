package com.villo.truco.social.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record DeclineFriendshipCommand(String username, PlayerId actorId) {

  public DeclineFriendshipCommand {

    Objects.requireNonNull(username);
    Objects.requireNonNull(actorId);
  }

  public DeclineFriendshipCommand(final String username, final String actorId) {

    this(username, PlayerId.of(actorId));
  }

}
