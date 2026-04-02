package com.villo.truco.auth.application.ports.out;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public interface AccessTokenIssuer {

  IssuedAccessToken issueForUser(PlayerId playerId);

  IssuedAccessToken issueForGuest(PlayerId playerId);

  record IssuedAccessToken(String value, long expiresIn) {

  }

}
