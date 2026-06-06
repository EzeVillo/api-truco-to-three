package com.villo.truco.social.application.dto;

import java.util.List;

public record FriendAvailabilityStateDTO(List<FriendAvailabilityDTO> friends) {

  public FriendAvailabilityStateDTO {

    friends = List.copyOf(friends);
  }

}
