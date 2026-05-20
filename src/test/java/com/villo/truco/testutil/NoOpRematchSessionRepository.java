package com.villo.truco.testutil;

import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * No-op RematchSessionRepository for use in tests that don't care about rematch logic.
 */
public final class NoOpRematchSessionRepository implements RematchSessionRepository {

  public static final NoOpRematchSessionRepository INSTANCE = new NoOpRematchSessionRepository();

  private NoOpRematchSessionRepository() {

  }

  @Override
  public Optional<RematchSession> findById(final RematchSessionId id) {

    return Optional.empty();
  }

  @Override
  public Optional<RematchSession> findByOriginMatchId(final MatchId matchId) {

    return Optional.empty();
  }

  @Override
  public Optional<RematchSession> findOpenByPlayer(final PlayerId playerId) {

    return Optional.empty();
  }

  @Override
  public List<RematchSession> findExpiredCandidates(final Instant now, final int batchSize) {

    return List.of();
  }

  @Override
  public void save(final RematchSession session) {

  }

}
