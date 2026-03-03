package com.villo.truco.infrastructure.http.dto.request;

import java.util.Objects;

public record JoinMatchRequest(String inviteCode) {

  public JoinMatchRequest {

    Objects.requireNonNull(inviteCode, "InviteCode is required");
  }

}
