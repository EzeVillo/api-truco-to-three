package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.LeaveTournamentCommand;
import com.villo.truco.application.ports.in.LeaveTournamentUseCase;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Objects;

public final class LeaveTournamentCommandHandler implements LeaveTournamentUseCase {

  private final TournamentResolver tournamentResolver;
  private final TournamentRepository tournamentRepository;

  public LeaveTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository) {

    this.tournamentResolver = Objects.requireNonNull(tournamentResolver);
    this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
  }

  @Override
  public Void handle(final LeaveTournamentCommand command) {

    final var tournament = this.tournamentResolver.resolve(command.tournamentId());

    tournament.leave(command.playerId());

    this.tournamentRepository.save(tournament);

    return null;
  }

}
