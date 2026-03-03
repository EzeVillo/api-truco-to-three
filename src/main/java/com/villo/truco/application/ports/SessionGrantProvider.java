package com.villo.truco.application.ports;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;

public interface SessionGrantProvider {

  String generateGrant(MatchId matchId, PlayerId playerId);

  PlayerIdentity validateAndConsumeGrant(String grant);

}
