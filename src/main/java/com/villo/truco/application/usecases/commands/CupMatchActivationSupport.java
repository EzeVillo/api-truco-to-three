package com.villo.truco.application.usecases.commands;

import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.BoutPairing;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.List;

final class CupMatchActivationSupport {

  private CupMatchActivationSupport() {

  }

  static void createAndLinkMatches(final Cup cup, final MatchRepository matchRepository,
      final List<BoutPairing> pairings) {

    final var matchRules = MatchRules.fromGamesToPlay(cup.getGamesToPlay());

    for (final var pairing : pairings) {
      final var match = Match.createReady(pairing.playerOne(), pairing.playerTwo(), matchRules);
      matchRepository.save(match);
      cup.linkBoutMatch(pairing.boutId(), match.getId());
    }
  }

}
