package com.villo.truco.infrastructure.persistence.inmemory;

import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public final class InMemorySpectatorshipRepository implements SpectatorshipRepository {

  private final ConcurrentMap<PlayerId, Spectatorship> spectatorships = new ConcurrentHashMap<>();

  @Override
  public Optional<Spectatorship> findBySpectatorId(final PlayerId spectatorId) {

    return Optional.ofNullable(this.spectatorships.get(spectatorId));
  }

  @Override
  public List<Spectatorship> findActiveByMatchId(final MatchId matchId) {

    return this.spectatorships.values().stream().filter(s -> s.isWatching(matchId)).toList();
  }

  @Override
  public Set<PlayerId> findActiveSpectatorIdsByMatchId(final MatchId matchId) {

    return this.spectatorships.values().stream().filter(s -> s.isWatching(matchId))
        .map(Spectatorship::getId).collect(Collectors.toSet());
  }

  @Override
  public int countActiveByMatchId(final MatchId matchId) {

    return (int) this.spectatorships.values().stream().filter(s -> s.isWatching(matchId)).count();
  }

  @Override
  public void save(final Spectatorship spectatorship) {

    this.spectatorships.put(spectatorship.getId(), spectatorship);
  }

}
