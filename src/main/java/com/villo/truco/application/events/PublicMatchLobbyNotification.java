package com.villo.truco.application.events;

import java.util.Map;

public record PublicMatchLobbyNotification(String eventType, long timestamp,
                                           Map<String, Object> payload) implements
    PostCommitApplicationEvent {

}
