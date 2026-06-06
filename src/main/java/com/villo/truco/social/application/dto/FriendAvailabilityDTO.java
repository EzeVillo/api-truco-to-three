package com.villo.truco.social.application.dto;

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

}
