package com.villo.truco.application.ports.out;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record MatchEventContext(MatchId matchId, PlayerId playerOne, PlayerId playerTwo) {

}
