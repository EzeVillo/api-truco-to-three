package com.villo.truco.infrastructure.websocket.dto;

import com.villo.truco.domain.model.league.events.LeagueAdvancedEvent;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.events.LeagueFinishedEvent;
import com.villo.truco.domain.model.league.events.LeagueFixtureActivatedEvent;
import com.villo.truco.domain.model.league.events.LeagueMatchActivatedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerForfeitedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerJoinedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerLeftEvent;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.LinkedHashMap;
import java.util.Map;

public record LeagueWsEvent(String eventType, long timestamp, Map<String, Object> payload) {

    private static final String LEAGUE_ID = "leagueId";

    public static LeagueWsEvent from(final DomainEventBase event) {

        final var payload = switch (event) {
            case LeaguePlayerJoinedEvent e -> mapPlayerJoined(e);
            case LeaguePlayerLeftEvent e -> mapPlayerLeft(e);
            case LeagueCancelledEvent e -> mapCancelled(e);
            case LeagueStartedEvent e -> mapStarted(e);
            case LeagueFixtureActivatedEvent e -> mapFixtureActivated(e);
            case LeagueMatchActivatedEvent e -> mapMatchActivated(e);
            case LeagueAdvancedEvent e -> mapAdvanced(e);
            case LeaguePlayerForfeitedEvent e -> mapPlayerForfeited(e);
            case LeagueFinishedEvent e -> mapFinished(e);
            default -> Map.<String, Object>of();
        };

        return new LeagueWsEvent(event.getEventType(), event.getTimestamp(), payload);
    }

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

    private static Map<String, Object> mapCancelled(final LeagueCancelledEvent event) {

        return Map.of(LEAGUE_ID, event.getLeagueId().value().toString());
    }

    private static Map<String, Object> mapStarted(final LeagueStartedEvent event) {

        return Map.of(LEAGUE_ID, event.getLeagueId().value().toString());
    }

    private static Map<String, Object> mapFixtureActivated(
        final LeagueFixtureActivatedEvent event) {

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
        map.put("leaders",
            event.getLeaders().stream().map(p -> p.value().toString()).toList());
        return map;
    }

}
