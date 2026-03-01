package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;

public interface MatchEventNotifier {

    void notifyPlayers(MatchId matchId, PlayerId playerOne, PlayerId playerTwo);

}
