package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.ForfeitCupCommand;
import com.villo.truco.application.ports.in.ForfeitCupUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class ForfeitCupCommandHandler implements ForfeitCupUseCase {

  private final CupResolver cupResolver;
  private final CupRepository cupRepository;
  private final MatchRepository matchRepository;

  public ForfeitCupCommandHandler(final CupResolver cupResolver, final CupRepository cupRepository,
      final MatchRepository matchRepository) {

    this.cupResolver = Objects.requireNonNull(cupResolver);
    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
  }

  @Override
  public Void handle(final ForfeitCupCommand command) {

    final var cup = this.cupResolver.resolve(command.cupId());

    final var result = cup.forfeitPlayer(command.forfeiter());

    final var matchRules = MatchRules.fromGamesToPlay(cup.getGamesToPlay());

    for (final var pairing : result.pendingPairings()) {
      final var match = Match.createReady(pairing.playerOne(), pairing.playerTwo(), matchRules);
      this.matchRepository.save(match);
      cup.linkBoutMatch(pairing.boutId(), match.getId());
    }

    this.cupRepository.save(cup);

    return null;
  }

}
