package com.villo.truco.social.application.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendActivityDTO;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FriendActivityNotification")
class FriendActivityContractTest {

  @Test
  @DisplayName("permite payload de snapshot con friends")
  void supportsStatePayload() {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("friends", List.of(new FriendActivityDTO("martina", null)));

    final var notification = new FriendActivityNotification(List.of(PlayerId.generate()),
        "FRIEND_ACTIVITY_STATE", 1L, payload);

    assertThat(notification.payload()).containsKey("friends");
  }

  @Test
  @DisplayName("permite payload de delta con spectatableMatch null")
  void supportsNullSpectatableMatch() {

    final var payload = new LinkedHashMap<String, Object>();
    payload.put("friendUsername", "martina");
    payload.put("spectatableMatch", null);

    final var notification = new FriendActivityNotification(List.of(PlayerId.generate()),
        "FRIEND_ACTIVITY_CHANGED", 1L, payload);

    assertThat(notification.payload()).containsEntry("spectatableMatch", null);
  }

}
