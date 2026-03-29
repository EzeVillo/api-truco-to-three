package com.villo.truco.application.eventhandlers;

import com.villo.truco.domain.model.league.events.LeagueAdvancedEvent;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.LeagueFinishedEvent;
import com.villo.truco.domain.model.league.events.LeagueFixtureActivatedEvent;
import com.villo.truco.domain.model.league.events.LeagueMatchActivatedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerForfeitedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerJoinedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerLeftEvent;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LeagueEventMapper {

  private static final String LEAGUE_ID = "leagueId";

  private static Map<String, Object> mapPlayerJoined(final LeaguePlayerJoinedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(LEAGUE_ID, event.getLeagueId().value().toString());
    map.put("playerId", event.getPlayerId().value().toString());
    return map;
  }

  private static Map<String, Object> mapPlayerLeft(final LeaguePlayerLeftEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(LEAGUE_ID, event.getLeagueId().value().toString());
    map.put("playerId", event.getPlayerId().value().toString());
    return map;
  }

  private static Map<String, Object> mapFixtureActivated(final LeagueFixtureActivatedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(LEAGUE_ID, event.getLeagueId().value().toString());
    map.put("fixtureId", event.getFixtureId().value().toString());
    return map;
  }

  private static Map<String, Object> mapMatchActivated(final LeagueMatchActivatedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(LEAGUE_ID, event.getLeagueId().value().toString());
    map.put("matchId", event.getMatchId().value().toString());
    return map;
  }

  private static Map<String, Object> mapAdvanced(final LeagueAdvancedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(LEAGUE_ID, event.getLeagueId().value().toString());
    if (event.getMatchId() != null) {
      map.put("matchId", event.getMatchId().value().toString());
    }
    map.put("winner", event.getWinner().value().toString());
    return map;
  }

  private static Map<String, Object> mapPlayerForfeited(final LeaguePlayerForfeitedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(LEAGUE_ID, event.getLeagueId().value().toString());
    map.put("forfeiter", event.getForfeiter().value().toString());
    return map;
  }

  private static Map<String, Object> mapFinished(final LeagueFinishedEvent event) {

    final var map = new LinkedHashMap<String, Object>();
    map.put(LEAGUE_ID, event.getLeagueId().value().toString());
    map.put("leaders", event.getLeaders().stream().map(p -> p.value().toString()).toList());
    return map;
  }

  public Map<String, Object> map(final LeagueDomainEvent event) {

    return switch (event) {
      case LeaguePlayerJoinedEvent e -> mapPlayerJoined(e);
      case LeaguePlayerLeftEvent e -> mapPlayerLeft(e);
      case LeagueCancelledEvent e -> Map.of(LEAGUE_ID, e.getLeagueId().value().toString());
      case LeagueStartedEvent e -> Map.of(LEAGUE_ID, e.getLeagueId().value().toString());
      case LeagueFixtureActivatedEvent e -> mapFixtureActivated(e);
      case LeagueMatchActivatedEvent e -> mapMatchActivated(e);
      case LeagueAdvancedEvent e -> mapAdvanced(e);
      case LeaguePlayerForfeitedEvent e -> mapPlayerForfeited(e);
      case LeagueFinishedEvent e -> mapFinished(e);
      default -> Map.of();
    };
  }

}
