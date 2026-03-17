package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class PlayCardCommandHandler implements PlayCardUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;

  public PlayCardCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
  }

  @Override
  public MatchId handle(final PlayCardCommand command) {

    final var match = this.matchResolver.resolve(command.matchId());

    match.playCard(command.playerId(), command.card());

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getId(), match.getPlayerOne(),
        match.getPlayerTwo(), match.getDomainEvents());
    match.clearDomainEvents();

    return command.matchId();
  }

}
