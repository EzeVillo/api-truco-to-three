package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.StartCupCommand;
import com.villo.truco.application.ports.in.StartCupUseCase;
import com.villo.truco.domain.model.cup.valueobjects.BoutStatus;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class StartCupCommandHandler implements StartCupUseCase {

  private final CupResolver cupResolver;
  private final CupRepository cupRepository;
  private final MatchRepository matchRepository;
  private final CupEventNotifier cupEventNotifier;

  public StartCupCommandHandler(final CupResolver cupResolver, final CupRepository cupRepository,
      final MatchRepository matchRepository, final CupEventNotifier cupEventNotifier) {

    this.cupResolver = Objects.requireNonNull(cupResolver);
    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.cupEventNotifier = Objects.requireNonNull(cupEventNotifier);
  }

  @Override
  public Void handle(final StartCupCommand command) {

    final var cup = this.cupResolver.resolve(command.cupId());

    cup.start(command.playerId());

    final var matchRules = MatchRules.fromGamesToPlay(cup.getGamesToPlay());

    for (final var bout : cup.getBouts()) {
      if (bout.status() != BoutStatus.PENDING) {
        continue;
      }

      final var match = Match.createReady(bout.playerOne(), bout.playerTwo(), matchRules);
      this.matchRepository.save(match);
      cup.linkBoutMatch(bout.boutId(), match.getId());
    }

    this.cupRepository.save(cup);

    this.cupEventNotifier.publishDomainEvents(cup.getCupDomainEvents());

    cup.clearDomainEvents();

    return null;
  }

}
