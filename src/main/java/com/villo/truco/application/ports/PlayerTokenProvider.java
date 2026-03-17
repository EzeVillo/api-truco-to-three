package com.villo.truco.application.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public interface PlayerTokenProvider {

  String generateAccessToken(PlayerId playerId);

}
