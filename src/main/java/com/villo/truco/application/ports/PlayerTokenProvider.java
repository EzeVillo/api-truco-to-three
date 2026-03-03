package com.villo.truco.application.ports;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;

public interface PlayerTokenProvider {

  String generateAccessToken(MatchId matchId, PlayerId playerId);

  PlayerIdentity validateAccessToken(String token);

  String generateRefreshToken(MatchId matchId, PlayerId playerId);

  PlayerIdentity validateRefreshToken(String token);

}
