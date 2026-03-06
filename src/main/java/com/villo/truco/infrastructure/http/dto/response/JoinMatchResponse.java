package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.JoinMatchDTO;

public record JoinMatchResponse(String accessToken) {

  public static JoinMatchResponse from(final JoinMatchDTO dto) {

    return new JoinMatchResponse(dto.accessToken());
  }

}
