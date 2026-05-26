package com.villo.truco.infrastructure.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

public record MatchWsEvent(String matchId, String eventType, long timestamp,
                           Map<String, Object> payload,
                           @JsonInclude(JsonInclude.Include.NON_NULL) Long stateVersion) {

}
