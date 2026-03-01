package com.villo.truco.application.commands;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;

public record JoinMatchCommand(MatchId matchId, PlayerId playerTwoId) {

    public JoinMatchCommand(final String matchId, final String playerTwoId) {

        this(MatchId.of(matchId), PlayerId.of(playerTwoId));
    }

}
