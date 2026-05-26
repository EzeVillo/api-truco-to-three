package com.villo.truco.infrastructure.websocket.dto;

import java.util.Map;

public sealed interface MatchWsEventBase permits MatchWsEvent, MatchDerivedWsEvent {

  String matchId();

  String eventType();

  long timestamp();

  Map<String, Object> payload();

}
