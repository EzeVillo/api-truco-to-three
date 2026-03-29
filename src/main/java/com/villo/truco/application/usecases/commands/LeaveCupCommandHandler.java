package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.LeaveCupCommand;
import com.villo.truco.application.ports.in.LeaveCupUseCase;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupRepository;
import java.util.Objects;

public final class LeaveCupCommandHandler implements LeaveCupUseCase {

  private final CupResolver cupResolver;
  private final CupRepository cupRepository;
  private final CupEventNotifier cupEventNotifier;

  public LeaveCupCommandHandler(final CupResolver cupResolver, final CupRepository cupRepository,
      final CupEventNotifier cupEventNotifier) {

    this.cupResolver = Objects.requireNonNull(cupResolver);
    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.cupEventNotifier = Objects.requireNonNull(cupEventNotifier);
  }

  @Override
  public Void handle(final LeaveCupCommand command) {

    final var cup = this.cupResolver.resolve(command.cupId());

    cup.leave(command.playerId());

    this.cupRepository.save(cup);

    this.cupEventNotifier.publishDomainEvents(cup.getCupDomainEvents());
    cup.clearDomainEvents();

    return null;
  }

}
