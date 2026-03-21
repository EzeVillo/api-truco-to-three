package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.StartLeagueCommand;
import com.villo.truco.application.ports.in.StartLeagueUseCase;
import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class StartLeagueCommandHandler implements StartLeagueUseCase {

  private final LeagueResolver leagueResolver;
  private final LeagueRepository leagueRepository;
  private final MatchRepository matchRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public StartLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository, final MatchRepository matchRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.leagueResolver = Objects.requireNonNull(leagueResolver);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public Void handle(final StartLeagueCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var league = this.leagueResolver.resolve(command.leagueId());

    league.start(command.playerId());

    final var matchRules = MatchRules.fromGamesToPlay(league.getGamesToPlay());

    for (final var fixture : league.getFixtures()) {
      if (fixture.status() == FixtureStatus.LIBRE) {
        continue;
      }

      final var match = Match.createReady(fixture.playerOne(), fixture.playerTwo(), matchRules);
      this.matchRepository.save(match);
      league.linkFixtureMatch(fixture.fixtureId(), match.getId());
    }

    this.leagueRepository.save(league);

    return null;
  }

}
