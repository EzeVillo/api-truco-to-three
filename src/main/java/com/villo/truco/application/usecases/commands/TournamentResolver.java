package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.exceptions.TournamentNotFoundException;
import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import java.util.Objects;

public final class TournamentResolver {

  private final TournamentQueryRepository tournamentQueryRepository;

  public TournamentResolver(final TournamentQueryRepository tournamentQueryRepository) {

    this.tournamentQueryRepository = Objects.requireNonNull(tournamentQueryRepository);
  }

  public Tournament resolve(final TournamentId tournamentId) {

    return this.tournamentQueryRepository.findById(tournamentId)
        .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
  }

  public Tournament resolve(final InviteCode inviteCode) {

    return this.tournamentQueryRepository.findByInviteCode(inviteCode)
        .orElseThrow(() -> new TournamentNotFoundException(inviteCode));
  }

}
