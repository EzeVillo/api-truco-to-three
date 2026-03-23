package com.villo.truco.domain.model.league;

import com.villo.truco.domain.model.league.valueobjects.FixtureId;
import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record FixtureSnapshot(FixtureId id, int matchdayNumber, PlayerId playerOne,
                              PlayerId playerTwo, MatchId matchId, PlayerId winner,
                              FixtureStatus status) {

}
