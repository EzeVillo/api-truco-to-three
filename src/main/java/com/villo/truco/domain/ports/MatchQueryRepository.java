package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import java.util.Optional;

public interface MatchQueryRepository {

    Optional<Match> findById(MatchId matchId);

}
