package com.villo.truco.social.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record RequestFriendshipCommand(PlayerId requesterId, String username) {

  public RequestFriendshipCommand {

    Objects.requireNonNull(requesterId);
    Objects.requireNonNull(username);
  }

  public RequestFriendshipCommand(final String requesterId, final String username) {

    this(PlayerId.of(requesterId), username);
  }

}
