package com.villo.truco.social.application.events;

import com.villo.truco.application.events.PostCommitApplicationEvent;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FriendActivityNotification(List<PlayerId> recipients, String eventType,
                                         long timestamp, Map<String, Object> payload) implements
    PostCommitApplicationEvent {

  public FriendActivityNotification {

    recipients = List.copyOf(recipients);
    payload = new LinkedHashMap<>(payload);
  }

}
