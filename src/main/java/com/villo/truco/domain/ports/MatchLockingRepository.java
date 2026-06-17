package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.Optional;

public interface MatchLockingRepository {

  Optional<Match> findByIdForUpdate(MatchId matchId);

}
