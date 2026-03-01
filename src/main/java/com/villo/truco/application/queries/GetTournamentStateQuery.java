package com.villo.truco.application.queries;

import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import java.util.Objects;

public record GetTournamentStateQuery(TournamentId tournamentId) {

    public GetTournamentStateQuery {

        Objects.requireNonNull(tournamentId);
    }

    public GetTournamentStateQuery(final String tournamentId) {

        this(TournamentId.of(tournamentId));
    }

}
