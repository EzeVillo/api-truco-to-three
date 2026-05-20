package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.ChooseRematchCommand;
import com.villo.truco.application.exceptions.RematchSessionNotFoundException;
import com.villo.truco.application.ports.in.ChooseRematchUseCase;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.time.Clock;
import java.util.Objects;

public final class ChooseRematchCommandHandler implements ChooseRematchUseCase {

  private final RematchSessionRepository repository;
  private final RematchSessionEventNotifier eventNotifier;
  private final Clock clock;

  public ChooseRematchCommandHandler(final RematchSessionRepository repository,
      final RematchSessionEventNotifier eventNotifier, final Clock clock) {

    this.repository = Objects.requireNonNull(repository);
    this.eventNotifier = Objects.requireNonNull(eventNotifier);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Void handle(final ChooseRematchCommand command) {

    final var session = repository.findByOriginMatchId(command.originMatchId())
        .orElseThrow(RematchSessionNotFoundException::new);
    session.chooseRematch(command.actor(), clock.instant(), MatchId.generate());
    repository.save(session);
    eventNotifier.publishDomainEvents(session.getRematchDomainEvents());
    session.clearDomainEvents();
    return null;
  }

}
