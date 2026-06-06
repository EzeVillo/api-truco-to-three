package com.villo.truco.social.application.dto;

import java.util.List;

public record FriendActivityStateDTO(List<FriendActivityDTO> friends) {

  public FriendActivityStateDTO {

    friends = List.copyOf(friends);
  }

}
