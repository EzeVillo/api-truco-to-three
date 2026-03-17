package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinTournamentCommand;
import com.villo.truco.application.dto.JoinTournamentDTO;
import com.villo.truco.application.ports.AggregateLockManager;
import com.villo.truco.application.ports.in.JoinTournamentUseCase;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentStatus;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Objects;

public final class JoinTournamentCommandHandler implements JoinTournamentUseCase {

  private final TournamentResolver tournamentResolver;
  private final TournamentRepository tournamentRepository;
  private final AggregateLockManager<TournamentId> tournamentLockManager;

  public JoinTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository,
      final AggregateLockManager<TournamentId> tournamentLockManager) {

    this.tournamentResolver = Objects.requireNonNull(tournamentResolver);
    this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
    this.tournamentLockManager = Objects.requireNonNull(tournamentLockManager);
  }

  @Override
  public JoinTournamentDTO handle(final JoinTournamentCommand command) {

    final var tournament = this.tournamentResolver.resolve(command.inviteCode());

    return this.tournamentLockManager.executeWithLock(tournament.getId(), () -> {
      tournament.join(command.playerId(), command.inviteCode());

      this.tournamentRepository.save(tournament);

      final var tournamentReady = tournament.getStatus() == TournamentStatus.WAITING_FOR_START;

      return new JoinTournamentDTO(tournament.getId().value().toString(), tournamentReady);
    });
  }

}
