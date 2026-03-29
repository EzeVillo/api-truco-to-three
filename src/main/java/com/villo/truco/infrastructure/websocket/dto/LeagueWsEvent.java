package com.villo.truco.infrastructure.websocket.dto;

import java.util.Map;

public record LeagueWsEvent(String leagueId, String eventType, long timestamp,
                            Map<String, Object> payload) {

}
