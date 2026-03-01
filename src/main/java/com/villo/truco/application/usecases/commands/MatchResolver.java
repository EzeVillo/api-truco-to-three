package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.MatchQueryRepository;
import java.util.Objects;

public final class MatchResolver {

    private final MatchQueryRepository matchQueryRepository;

    public MatchResolver(final MatchQueryRepository matchQueryRepository) {

        this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    }

    public Match resolve(final MatchId matchId) {

        return this.matchQueryRepository.findById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));
    }

}
