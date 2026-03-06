package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateMatchDTO;

public record CreateMatchResponse(String matchId, String accessToken, String inviteCode) {

  public static CreateMatchResponse from(final CreateMatchDTO dto) {

    return new CreateMatchResponse(dto.matchId(), dto.accessToken(), dto.inviteCode());
  }

}
