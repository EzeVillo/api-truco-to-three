package com.villo.truco.domain.model.cup;

import com.villo.truco.domain.model.cup.valueobjects.BoutId;
import com.villo.truco.domain.model.cup.valueobjects.BoutStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record BoutView(BoutId boutId, int roundNumber, int bracketPosition, PlayerId playerOne,
                       PlayerId playerTwo, MatchId matchId, PlayerId winner, BoutStatus status) {

}
