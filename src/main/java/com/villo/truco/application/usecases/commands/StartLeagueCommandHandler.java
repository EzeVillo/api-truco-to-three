package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.StartLeagueCommand;
import com.villo.truco.application.ports.in.StartLeagueUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class StartLeagueCommandHandler implements StartLeagueUseCase {

  private final LeagueResolver leagueResolver;
  private final LeagueRepository leagueRepository;
  private final MatchRepository matchRepository;
  private final LeagueEventNotifier leagueEventNotifier;

  public StartLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository, final MatchRepository matchRepository,
      final LeagueEventNotifier leagueEventNotifier) {

    this.leagueResolver = Objects.requireNonNull(leagueResolver);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.leagueEventNotifier = Objects.requireNonNull(leagueEventNotifier);
  }

  @Override
  public Void handle(final StartLeagueCommand command) {

    final var league = this.leagueResolver.resolve(command.leagueId());

    final var matchRules = MatchRules.fromGamesToPlay(league.getGamesToPlay());

    for (final var activation : league.start(command.playerId())) {
      final var match = Match.createReady(activation.playerOne(), activation.playerTwo(),
          matchRules);
      this.matchRepository.save(match);
      league.linkFixtureMatch(activation.fixtureId(), match.getId());
    }

    this.leagueRepository.save(league);

    this.leagueEventNotifier.publishDomainEvents(league.getLeagueDomainEvents());

    league.clearDomainEvents();

    return null;
  }

}
