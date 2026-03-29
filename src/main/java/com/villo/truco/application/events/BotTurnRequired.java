package com.villo.truco.application.events;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record BotTurnRequired(MatchId matchId, PlayerId botPlayerId) implements
    PostCommitApplicationEvent {

}
