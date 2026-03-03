package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.SessionTokenDTO;

public record SessionTokenResponse(String accessToken, String refreshToken) {

  public static SessionTokenResponse from(final SessionTokenDTO dto) {

    return new SessionTokenResponse(dto.accessToken(), dto.refreshToken());
  }

}
