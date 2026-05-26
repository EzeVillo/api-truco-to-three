package com.villo.truco.infrastructure.websocket.dto;

import java.util.Map;

public record MatchDerivedWsEvent(String matchId, String eventType, long timestamp,
                                  Map<String, Object> payload) implements MatchWsEventBase {

}
