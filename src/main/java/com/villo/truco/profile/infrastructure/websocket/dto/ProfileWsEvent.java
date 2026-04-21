package com.villo.truco.profile.infrastructure.websocket.dto;

import java.util.Map;

public record ProfileWsEvent(String eventType, long timestamp, Map<String, Object> payload) {

}
