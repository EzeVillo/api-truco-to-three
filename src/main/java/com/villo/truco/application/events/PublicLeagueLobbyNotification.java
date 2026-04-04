package com.villo.truco.application.events;

import java.util.Map;

public record PublicLeagueLobbyNotification(String eventType, long timestamp,
                                            Map<String, Object> payload) implements
    PostCommitApplicationEvent {

}
