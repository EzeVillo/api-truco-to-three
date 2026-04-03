package com.villo.truco.application.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Map;
import java.util.Set;

public record SpectatorMatchEventNotification(MatchId matchId, Set<PlayerId> spectatorIds,
                                              String eventType, long timestamp,
                                              Map<String, Object> payload) implements
    ApplicationEvent {

}
