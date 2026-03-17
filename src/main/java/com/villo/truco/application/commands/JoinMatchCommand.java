package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record JoinMatchCommand(PlayerId playerId, InviteCode inviteCode) {

  public JoinMatchCommand(final String playerId, final String inviteCode) {

    this(PlayerId.of(playerId), InviteCode.of(inviteCode));
  }

}
