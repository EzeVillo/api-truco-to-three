package com.villo.truco.infrastructure.websocket.dto;

import com.villo.truco.domain.model.cup.events.CupAdvancedEvent;
import com.villo.truco.domain.model.cup.events.CupBoutActivatedEvent;
import com.villo.truco.domain.model.cup.events.CupCancelledEvent;
import com.villo.truco.domain.model.cup.events.CupFinishedEvent;
import com.villo.truco.domain.model.cup.events.CupMatchActivatedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerForfeitedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerJoinedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerLeftEvent;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.LinkedHashMap;
import java.util.Map;

public record CupWsEvent(String cupId, String eventType, long timestamp,
                         Map<String, Object> payload) {

    public static CupWsEvent from(final DomainEventBase event, final CupId cupId) {

        final var payload = switch (event) {
            case CupPlayerJoinedEvent e -> mapPlayerJoined(e);
            case CupPlayerLeftEvent e -> mapPlayerLeft(e);
            case CupCancelledEvent e -> mapCancelled(e);
            case CupStartedEvent e -> mapStarted(e);
            case CupBoutActivatedEvent e -> mapBoutActivated(e);
            case CupMatchActivatedEvent e -> mapMatchActivated(e);
            case CupAdvancedEvent e -> mapAdvanced(e);
            case CupPlayerForfeitedEvent e -> mapPlayerForfeited(e);
            case CupFinishedEvent e -> mapFinished(e);
            default -> Map.<String, Object>of();
        };

        return new CupWsEvent(cupId.value().toString(), event.getEventType(),
            event.getTimestamp(), payload);
    }

    private static Map<String, Object> mapPlayerJoined(final CupPlayerJoinedEvent event) {

        final var map = new LinkedHashMap<String, Object>();
        map.put("playerId", event.getPlayerId().value().toString());
        return map;
    }

    private static Map<String, Object> mapPlayerLeft(final CupPlayerLeftEvent event) {

        final var map = new LinkedHashMap<String, Object>();
        map.put("playerId", event.getPlayerId().value().toString());
        return map;
    }

    private static Map<String, Object> mapCancelled(final CupCancelledEvent event) {

        return Map.of();
    }

    private static Map<String, Object> mapStarted(final CupStartedEvent event) {

        return Map.of();
    }

    private static Map<String, Object> mapBoutActivated(final CupBoutActivatedEvent event) {

        return Map.of("boutId", event.getBoutId().value().toString());
    }

    private static Map<String, Object> mapMatchActivated(final CupMatchActivatedEvent event) {

        return Map.of("matchId", event.getMatchId().value().toString());
    }

    private static Map<String, Object> mapAdvanced(final CupAdvancedEvent event) {

        final var map = new LinkedHashMap<String, Object>();
        if (event.getMatchId() != null) {
            map.put("matchId", event.getMatchId().value().toString());
        }
        map.put("winner", event.getWinner().value().toString());
        return map;
    }

    private static Map<String, Object> mapPlayerForfeited(final CupPlayerForfeitedEvent event) {

        return Map.of("forfeiter", event.getForfeiter().value().toString());
    }

    private static Map<String, Object> mapFinished(final CupFinishedEvent event) {

        return Map.of("champion", event.getChampion().value().toString());
    }

}
