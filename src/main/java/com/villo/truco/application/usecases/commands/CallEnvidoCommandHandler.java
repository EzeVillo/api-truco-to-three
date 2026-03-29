package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.Objects;

public final class CallEnvidoCommandHandler implements CallEnvidoUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;

  public CallEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
  }

  @Override
  public MatchId handle(final CallEnvidoCommand command) {

    final var match = this.matchResolver.resolve(command.matchId());

    match.callEnvido(command.playerId(), command.call());

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();

    return command.matchId();
  }

}
