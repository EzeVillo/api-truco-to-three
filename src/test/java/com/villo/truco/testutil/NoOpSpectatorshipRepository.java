package com.villo.truco.testutil;

import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * No-op SpectatorshipRepository for use in tests that don't care about spectating logic.
 */
public final class NoOpSpectatorshipRepository implements SpectatorshipRepository {

  public static final NoOpSpectatorshipRepository INSTANCE = new NoOpSpectatorshipRepository();

  private NoOpSpectatorshipRepository() {

  }

  @Override
  public Optional<Spectatorship> findBySpectatorId(final PlayerId spectatorId) {

    return Optional.empty();
  }

  @Override
  public List<Spectatorship> findActiveByMatchId(final MatchId matchId) {

    return List.of();
  }

  @Override
  public Set<PlayerId> findActiveSpectatorIdsByMatchId(final MatchId matchId) {

    return Set.of();
  }

  @Override
  public int countActiveByMatchId(final MatchId matchId) {

    return 0;
  }

  @Override
  public void save(final Spectatorship spectatorship) {

  }

}
