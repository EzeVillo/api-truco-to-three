package com.villo.truco.infrastructure.websocket.dto;

import java.util.Map;

public record CupWsEvent(String eventType, long timestamp, Map<String, Object> payload) {

}
