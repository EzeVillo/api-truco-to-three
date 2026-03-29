package com.villo.truco.application.eventhandlers;

import com.villo.truco.domain.model.cup.events.CupAdvancedEvent;
import com.villo.truco.domain.model.cup.events.CupBoutActivatedEvent;
import com.villo.truco.domain.model.cup.events.CupCancelledEvent;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.model.cup.events.CupFinishedEvent;
import com.villo.truco.domain.model.cup.events.CupMatchActivatedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerForfeitedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerJoinedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerLeftEvent;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CupEventMapper {

  private static final String CUP_ID = "cupId";

  private static Map<String, Object> mapPlayerJoined(final CupPlayerJoinedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("playerId", event.getPlayerId().value().toString());
    return map;
  }

  private static Map<String, Object> mapPlayerLeft(final CupPlayerLeftEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("playerId", event.getPlayerId().value().toString());
    return map;
  }

  private static Map<String, Object> mapBoutActivated(final CupBoutActivatedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("boutId", event.getBoutId().value().toString());
    return map;
  }

  private static Map<String, Object> mapMatchActivated(final CupMatchActivatedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("matchId", event.getMatchId().value().toString());
    return map;
  }

  private static Map<String, Object> mapAdvanced(final CupAdvancedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    if (event.getMatchId() != null) {
      map.put("matchId", event.getMatchId().value().toString());
    }
    map.put("winner", event.getWinner().value().toString());
    return map;
  }

  private static Map<String, Object> mapPlayerForfeited(final CupPlayerForfeitedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("forfeiter", event.getForfeiter().value().toString());
    return map;
  }

  private static Map<String, Object> mapFinished(final CupFinishedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("champion", event.getChampion().value().toString());
    return map;
  }

  public Map<String, Object> map(final CupDomainEvent event) {

    return switch (event) {
      case CupPlayerJoinedEvent e -> mapPlayerJoined(e);
      case CupPlayerLeftEvent e -> mapPlayerLeft(e);
      case CupCancelledEvent e -> Map.of(CUP_ID, e.getCupId().value().toString());
      case CupStartedEvent e -> Map.of(CUP_ID, e.getCupId().value().toString());
      case CupBoutActivatedEvent e -> mapBoutActivated(e);
      case CupMatchActivatedEvent e -> mapMatchActivated(e);
      case CupAdvancedEvent e -> mapAdvanced(e);
      case CupPlayerForfeitedEvent e -> mapPlayerForfeited(e);
      case CupFinishedEvent e -> mapFinished(e);
      default -> Map.of();
    };
  }

}
