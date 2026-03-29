package com.villo.truco.infrastructure.websocket.dto;

import java.util.Map;

public record ChatWsEvent(String chatId, String eventType, long timestamp,
                          Map<String, Object> payload) {

}
