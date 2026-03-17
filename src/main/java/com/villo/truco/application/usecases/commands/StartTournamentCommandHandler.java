package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.StartTournamentCommand;
import com.villo.truco.application.ports.AggregateLockManager;
import com.villo.truco.application.ports.in.StartTournamentUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Objects;

public final class StartTournamentCommandHandler implements StartTournamentUseCase {

  private final TournamentResolver tournamentResolver;
  private final TournamentRepository tournamentRepository;
  private final MatchRepository matchRepository;
  private final AggregateLockManager<TournamentId> tournamentLockManager;

  public StartTournamentCommandHandler(final TournamentResolver tournamentResolver,
      final TournamentRepository tournamentRepository, final MatchRepository matchRepository,
      final AggregateLockManager<TournamentId> tournamentLockManager) {

    this.tournamentResolver = Objects.requireNonNull(tournamentResolver);
    this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.tournamentLockManager = Objects.requireNonNull(tournamentLockManager);
  }

  @Override
  public void handle(final StartTournamentCommand command) {

    final var tournament = this.tournamentResolver.resolve(command.tournamentId());

    this.tournamentLockManager.executeWithLock(tournament.getId(), () -> {
      tournament.start(command.playerId());

      final var matchRules = MatchRules.fromGamesToPlay(tournament.getGamesToPlay());

      for (final var fixture : tournament.getFixtures()) {
        if (fixture.status() == FixtureStatus.LIBRE) {
          continue;
        }

        final var match = Match.createReady(fixture.playerOne(), fixture.playerTwo(), matchRules);
        this.matchRepository.save(match);
        tournament.linkFixtureMatch(fixture.fixtureId(), match.getId());
      }

      this.tournamentRepository.save(tournament);
    });
  }

}
