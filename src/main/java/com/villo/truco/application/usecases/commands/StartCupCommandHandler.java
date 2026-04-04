package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.StartCupCommand;
import com.villo.truco.application.ports.in.StartCupUseCase;
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
    final var pairings = cup.start(command.playerId());
    CupMatchActivationSupport.createAndLinkMatches(cup, this.matchRepository, pairings);

    this.cupRepository.save(cup);

    this.cupEventNotifier.publishDomainEvents(cup.getCupDomainEvents());

    cup.clearDomainEvents();

    return null;
  }

}
