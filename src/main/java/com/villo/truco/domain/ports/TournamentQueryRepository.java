package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import java.util.Optional;

public interface TournamentQueryRepository {

    Optional<Tournament> findById(TournamentId tournamentId);

}
