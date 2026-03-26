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
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.LinkedHashMap;
import java.util.Map;

public record LeagueWsEvent(String leagueId, String eventType, long timestamp,
                            Map<String, Object> payload) {

    public static LeagueWsEvent from(final DomainEventBase event, final LeagueId leagueId) {

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

        return new LeagueWsEvent(leagueId.value().toString(), event.getEventType(),
            event.getTimestamp(), payload);
    }

    private static Map<String, Object> mapPlayerJoined(final LeaguePlayerJoinedEvent event) {

        final var map = new LinkedHashMap<String, Object>();
        map.put("playerId", event.getPlayerId().value().toString());
        return map;
    }

    private static Map<String, Object> mapPlayerLeft(final LeaguePlayerLeftEvent event) {

        final var map = new LinkedHashMap<String, Object>();
        map.put("playerId", event.getPlayerId().value().toString());
        return map;
    }

    private static Map<String, Object> mapCancelled(final LeagueCancelledEvent event) {

        return Map.of();
    }

    private static Map<String, Object> mapStarted(final LeagueStartedEvent event) {

        return Map.of();
    }

    private static Map<String, Object> mapFixtureActivated(
        final LeagueFixtureActivatedEvent event) {

        return Map.of("fixtureId", event.getFixtureId().value().toString());
    }

    private static Map<String, Object> mapMatchActivated(final LeagueMatchActivatedEvent event) {

        return Map.of("matchId", event.getMatchId().value().toString());
    }

    private static Map<String, Object> mapAdvanced(final LeagueAdvancedEvent event) {

        final var map = new LinkedHashMap<String, Object>();
        if (event.getMatchId() != null) {
            map.put("matchId", event.getMatchId().value().toString());
        }
        map.put("winner", event.getWinner().value().toString());
        return map;
    }

    private static Map<String, Object> mapPlayerForfeited(final LeaguePlayerForfeitedEvent event) {

        return Map.of("forfeiter", event.getForfeiter().value().toString());
    }

    private static Map<String, Object> mapFinished(final LeagueFinishedEvent event) {

        return Map.of("leaders",
            event.getLeaders().stream().map(p -> p.value().toString()).toList());
    }

}
