package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.RegisterTournamentMatchResultCommand;
import com.villo.truco.application.ports.in.RegisterTournamentMatchResultUseCase;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.tournament.exceptions.MatchNotPartOfTournamentException;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Objects;

public final class RegisterTournamentMatchResultCommandHandler implements
    RegisterTournamentMatchResultUseCase {

  private final TournamentResolver tournamentResolver;
  private final TournamentRepository tournamentRepository;
  private final MatchQueryRepository matchQueryRepository;

  public RegisterTournamentMatchResultCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository,
      final MatchQueryRepository matchQueryRepository) {

    this.tournamentResolver = Objects.requireNonNull(tournamentResolver);
    this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
  }

  @Override
  public Void handle(final RegisterTournamentMatchResultCommand command) {

    final var tournament = this.tournamentResolver.resolve(command.tournamentId());

    final var match = this.matchQueryRepository.findById(command.matchId())
        .orElseThrow(() -> new MatchNotPartOfTournamentException(command.matchId()));

    if (match.getStatus() != MatchStatus.FINISHED || match.getMatchWinner() == null) {
      return null;
    }

    tournament.recordMatchWinner(command.matchId(), match.getMatchWinner());
    this.tournamentRepository.save(tournament);

    return null;
  }

}
