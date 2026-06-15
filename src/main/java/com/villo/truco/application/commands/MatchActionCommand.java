package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public interface MatchActionCommand {

  MatchId matchId();

  PlayerId playerId();

}
