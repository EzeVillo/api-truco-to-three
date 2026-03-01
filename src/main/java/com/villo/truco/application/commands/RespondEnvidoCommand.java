package com.villo.truco.application.commands;

import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.util.Objects;

public record RespondEnvidoCommand(MatchId matchId, PlayerId playerId, EnvidoResponse response) {

    public RespondEnvidoCommand {

        Objects.requireNonNull(matchId, "MatchId is required");
        Objects.requireNonNull(playerId, "PlayerId is required");
        Objects.requireNonNull(response, "Response is required");
    }

    public RespondEnvidoCommand(final String matchId, final String playerId,
        final String response) {

        this(MatchId.of(matchId), PlayerId.of(playerId), EnvidoResponse.valueOf(response));
    }

}
