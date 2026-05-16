package com.villo.truco.profile.domain.ports;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public interface ProcessedMatchStatsRegistry {

  boolean tryRegister(PlayerId playerId, MatchId matchId);

}
