package com.villo.truco.infrastructure.websocket.dto;

import java.util.Map;

public record CupWsEvent(String cupId, String eventType, long timestamp,
                         Map<String, Object> payload) {

}
