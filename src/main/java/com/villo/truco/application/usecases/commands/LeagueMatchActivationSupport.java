package com.villo.truco.application.usecases.commands;

import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.FixtureActivation;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.List;

final class LeagueMatchActivationSupport {

  private LeagueMatchActivationSupport() {

  }

  static void createAndLinkInitialMatches(final League league,
      final MatchRepository matchRepository, final List<FixtureActivation> activations) {

    final var matchRules = MatchRules.fromGamesToPlay(league.getGamesToPlay());

    for (final var activation : activations) {
      final var match = Match.createReady(activation.playerOne(), activation.playerTwo(),
          matchRules);
      matchRepository.save(match);
      league.linkFixtureMatch(activation.fixtureId(), match.getId());
    }
  }

}
