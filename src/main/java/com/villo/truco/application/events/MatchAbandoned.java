package com.villo.truco.application.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record MatchAbandoned(MatchId matchId, PlayerId winnerId, PlayerId abandonerId) implements
    ApplicationEvent {

}
