package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.AbandonMatchCommand;
import com.villo.truco.application.ports.in.AbandonMatchUseCase;
import com.villo.truco.domain.model.match.exceptions.PlayerNotInMatchException;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class AbandonMatchCommandHandler implements AbandonMatchUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;

  public AbandonMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
  }

  @Override
  public Void handle(final AbandonMatchCommand command) {

    final var match = this.matchResolver.resolve(command.matchId());

    final PlayerId abandoner = command.playerId();
    final PlayerId winner;

    if (abandoner.equals(match.getPlayerOne())) {
      winner = match.getPlayerTwo();
    } else if (abandoner.equals(match.getPlayerTwo())) {
      winner = match.getPlayerOne();
    } else {
      throw new PlayerNotInMatchException(abandoner);
    }

    match.forfeit(winner);

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
        match.getPlayerTwo(), match.getDomainEvents());
    match.clearDomainEvents();

    return null;
  }

}
