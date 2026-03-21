package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record JoinCupCommand(PlayerId playerId, InviteCode inviteCode) {

  public JoinCupCommand(final String playerId, final String inviteCode) {

    this(PlayerId.of(playerId), InviteCode.of(inviteCode));
  }

}
