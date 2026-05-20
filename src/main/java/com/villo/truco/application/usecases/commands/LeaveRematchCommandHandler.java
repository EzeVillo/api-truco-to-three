package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.LeaveRematchCommand;
import com.villo.truco.application.exceptions.RematchSessionNotFoundException;
import com.villo.truco.application.ports.in.LeaveRematchUseCase;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import com.villo.truco.domain.ports.RematchSessionRepository;
import java.util.Objects;

public final class LeaveRematchCommandHandler implements LeaveRematchUseCase {

  private final RematchSessionRepository repository;
  private final RematchSessionEventNotifier eventNotifier;

  public LeaveRematchCommandHandler(final RematchSessionRepository repository,
      final RematchSessionEventNotifier eventNotifier) {

    this.repository = Objects.requireNonNull(repository);
    this.eventNotifier = Objects.requireNonNull(eventNotifier);
  }

  @Override
  public Void handle(final LeaveRematchCommand command) {

    final var session = repository.findByOriginMatchId(command.originMatchId())
        .orElseThrow(RematchSessionNotFoundException::new);
    session.leave(command.actor());
    repository.save(session);
    eventNotifier.publishDomainEvents(session.getRematchDomainEvents());
    session.clearDomainEvents();
    return null;
  }

}
