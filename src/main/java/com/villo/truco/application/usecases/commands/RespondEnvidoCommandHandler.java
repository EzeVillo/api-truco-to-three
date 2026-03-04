package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResult;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;
import java.util.Optional;

public final class RespondEnvidoCommandHandler implements RespondEnvidoUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;

  public RespondEnvidoCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
  }

  @Override
  public Optional<EnvidoResult> handle(final RespondEnvidoCommand command) {

    final var match = this.matchResolver.resolve(command.matchId());

    final Optional<EnvidoResult> result = switch (command.response()) {
      case QUIERO -> Optional.of(match.acceptEnvido(command.playerId()));
      case NO_QUIERO -> {
        match.rejectEnvido(command.playerId());
        yield Optional.empty();
      }
    };

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
        match.getPlayerTwo(), match.getDomainEvents());
    match.clearDomainEvents();

    return result;
  }

}
