package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import java.util.Optional;

public interface TournamentQueryRepository {

  Optional<Tournament> findById(TournamentId tournamentId);

  Optional<Tournament> findByInviteCode(InviteCode inviteCode);

}
