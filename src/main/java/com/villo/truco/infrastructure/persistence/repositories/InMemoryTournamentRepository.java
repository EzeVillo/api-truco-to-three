package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public final class InMemoryTournamentRepository
    implements TournamentRepository, TournamentQueryRepository {

    private final Map<TournamentId, Tournament> store = new ConcurrentHashMap<>();

    @Override
    public void save(final Tournament tournament) {

        this.store.put(tournament.getId(), tournament);
    }

    @Override
    public Optional<Tournament> findById(final TournamentId tournamentId) {

        return Optional.ofNullable(this.store.get(tournamentId));
    }

}
