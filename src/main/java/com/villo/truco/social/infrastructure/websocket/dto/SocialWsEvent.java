package com.villo.truco.social.infrastructure.websocket.dto;

import java.util.Map;

public record SocialWsEvent(String eventType, long timestamp, Map<String, Object> payload) {

}
