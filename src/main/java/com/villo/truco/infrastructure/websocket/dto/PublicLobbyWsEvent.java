package com.villo.truco.infrastructure.websocket.dto;

import java.util.Map;

public record PublicLobbyWsEvent(String eventType, long timestamp, Map<String, Object> payload) {

}
