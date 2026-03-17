package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.LeaveTournamentCommand;
import com.villo.truco.application.ports.AggregateLockManager;
import com.villo.truco.application.ports.in.LeaveTournamentUseCase;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Objects;

public final class LeaveTournamentCommandHandler implements LeaveTournamentUseCase {

  private final TournamentResolver tournamentResolver;
  private final TournamentRepository tournamentRepository;
  private final AggregateLockManager<TournamentId> tournamentLockManager;

  public LeaveTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository,
      final AggregateLockManager<TournamentId> tournamentLockManager) {

    this.tournamentResolver = Objects.requireNonNull(tournamentResolver);
    this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
    this.tournamentLockManager = Objects.requireNonNull(tournamentLockManager);
  }

  @Override
  public void handle(final LeaveTournamentCommand command) {

    final var tournament = this.tournamentResolver.resolve(command.tournamentId());

    this.tournamentLockManager.executeWithLock(tournament.getId(), () -> {
      tournament.leave(command.playerId());

      this.tournamentRepository.save(tournament);
    });
  }

}
