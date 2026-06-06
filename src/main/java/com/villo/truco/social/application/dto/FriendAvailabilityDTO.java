package com.villo.truco.social.application.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record FriendAvailabilityDTO(String friendUsername, boolean online,
                                    FriendAvailabilityStatus availability,
                                    FriendBusyReason busyReason,
                                    SpectatableMatchRefDTO spectatableMatch) {

  public FriendAvailabilityDTO {

    if (availability == FriendAvailabilityStatus.AVAILABLE && busyReason != null) {
      throw new IllegalArgumentException("Available friends cannot have a busy reason");
    }
    if (availability == FriendAvailabilityStatus.BUSY && busyReason == null) {
      throw new IllegalArgumentException("Busy friends must have a busy reason");
    }
  }

  public Map<String, Object> toPayload() {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("friendUsername", this.friendUsername);
    payload.put("online", this.online);
    payload.put("availability", this.availability);
    payload.put("busyReason", this.busyReason);
    payload.put("spectatableMatch", this.spectatableMatch);
    return payload;
  }

}
