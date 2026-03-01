package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public final class InMemoryMatchRepository implements MatchRepository, MatchQueryRepository {

    private final Map<MatchId, Match> store = new ConcurrentHashMap<>();

    @Override
    public void save(final Match match) {

        this.store.put(match.getId(), match);
    }

    @Override
    public Optional<Match> findById(final MatchId id) {

        return Optional.ofNullable(this.store.get(id));
    }

}
