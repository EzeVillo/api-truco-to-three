package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public final class InMemoryTournamentRepository implements TournamentRepository,
    TournamentQueryRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryTournamentRepository.class);

  private final Map<TournamentId, Tournament> store = new ConcurrentHashMap<>();

  @Override
  public void save(final Tournament tournament) {

    this.store.put(tournament.getId(), tournament);
    LOGGER.debug("Tournament saved in memory: tournamentId={}, totalStored={}", tournament.getId(),
        this.store.size());
  }

  @Override
  public Optional<Tournament> findById(final TournamentId tournamentId) {

    final var tournament = this.store.get(tournamentId);
    LOGGER.debug("Tournament lookup: tournamentId={}, found={}", tournamentId, tournament != null);
    return Optional.ofNullable(tournament);
  }

  @Override
  public Optional<Tournament> findByInviteCode(final InviteCode inviteCode) {

    return this.store.values().stream()
        .filter(tournament -> tournament.getInviteCode().equals(inviteCode)).findFirst();
  }

}
