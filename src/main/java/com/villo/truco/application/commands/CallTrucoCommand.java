package com.villo.truco.application.commands;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.util.Objects;

public record CallTrucoCommand(MatchId matchId, PlayerId playerId) {

    public CallTrucoCommand {

        Objects.requireNonNull(matchId);
        Objects.requireNonNull(playerId);
    }

    public CallTrucoCommand(final String matchId, final String playerId) {

        this(MatchId.of(matchId), PlayerId.of(playerId));
    }

}
