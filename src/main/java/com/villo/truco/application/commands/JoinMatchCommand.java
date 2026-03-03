package com.villo.truco.application.commands;

import com.villo.truco.domain.model.match.valueobjects.InviteCode;
import com.villo.truco.domain.model.match.valueobjects.MatchId;

public record JoinMatchCommand(MatchId matchId, InviteCode inviteCode) {

  public JoinMatchCommand(final String matchId, final String inviteCode) {

    this(MatchId.of(matchId), InviteCode.of(inviteCode));
  }

}
