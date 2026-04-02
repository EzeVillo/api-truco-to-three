package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.PublicActorResolver;
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
import java.util.Objects;

public final class CupEventMapper {

  private static final String CUP_ID = "cupId";
  private final PublicActorResolver publicActorResolver;

  public CupEventMapper(final PublicActorResolver publicActorResolver) {

    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
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

  private Map<String, Object> mapPlayerJoined(final CupPlayerJoinedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("player", this.publicActorResolver.resolve(event.getPlayerId()));
    return map;
  }

  private Map<String, Object> mapPlayerLeft(final CupPlayerLeftEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("player", this.publicActorResolver.resolve(event.getPlayerId()));
    return map;
  }

  private Map<String, Object> mapAdvanced(final CupAdvancedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    if (event.getMatchId() != null) {
      map.put("matchId", event.getMatchId().value().toString());
    }
    map.put("winner", this.publicActorResolver.resolve(event.getWinner()));
    return map;
  }

  private Map<String, Object> mapPlayerForfeited(final CupPlayerForfeitedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("forfeiter", this.publicActorResolver.resolve(event.getForfeiter()));
    return map;
  }

  private Map<String, Object> mapFinished(final CupFinishedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(CUP_ID, event.getCupId().value().toString());
    map.put("champion", this.publicActorResolver.resolve(event.getChampion()));
    return map;
  }

  public Map<String, Object> map(final CupDomainEvent event) {

    return switch (event) {
      case CupPlayerJoinedEvent e -> this.mapPlayerJoined(e);
      case CupPlayerLeftEvent e -> this.mapPlayerLeft(e);
      case CupCancelledEvent e -> Map.of(CUP_ID, e.getCupId().value().toString());
      case CupStartedEvent e -> Map.of(CUP_ID, e.getCupId().value().toString());
      case CupBoutActivatedEvent e -> mapBoutActivated(e);
      case CupMatchActivatedEvent e -> mapMatchActivated(e);
      case CupAdvancedEvent e -> this.mapAdvanced(e);
      case CupPlayerForfeitedEvent e -> this.mapPlayerForfeited(e);
      case CupFinishedEvent e -> this.mapFinished(e);
      default -> Map.of();
    };
  }

}
