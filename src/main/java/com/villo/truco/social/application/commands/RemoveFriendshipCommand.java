package com.villo.truco.social.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record RemoveFriendshipCommand(String username, PlayerId actorId) {

  public RemoveFriendshipCommand {

    Objects.requireNonNull(username);
    Objects.requireNonNull(actorId);
  }

  public RemoveFriendshipCommand(final String username, final String actorId) {

    this(username, PlayerId.of(actorId));
  }

}
