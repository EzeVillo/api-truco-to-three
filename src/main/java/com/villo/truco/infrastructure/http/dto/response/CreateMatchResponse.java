package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.CreateMatchDTO;

public record CreateMatchResponse(String matchId, String sessionGrant, String inviteCode) {

  public static CreateMatchResponse from(final CreateMatchDTO dto) {

    return new CreateMatchResponse(dto.matchId(), dto.sessionGrant(), dto.inviteCode());
  }

}
