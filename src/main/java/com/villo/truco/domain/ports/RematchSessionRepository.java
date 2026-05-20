package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RematchSessionRepository {

  Optional<RematchSession> findById(RematchSessionId id);

  Optional<RematchSession> findByOriginMatchId(MatchId matchId);

  Optional<RematchSession> findOpenByPlayer(PlayerId playerId);

  List<RematchSession> findExpiredCandidates(Instant now, int batchSize);

  void save(RematchSession session);

}
