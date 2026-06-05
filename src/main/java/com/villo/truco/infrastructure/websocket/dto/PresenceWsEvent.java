package com.villo.truco.infrastructure.websocket.dto;

import com.villo.truco.infrastructure.http.dto.response.UserPresenceResponse;

public record PresenceWsEvent(String eventType, long timestamp, UserPresenceResponse payload) {

}
