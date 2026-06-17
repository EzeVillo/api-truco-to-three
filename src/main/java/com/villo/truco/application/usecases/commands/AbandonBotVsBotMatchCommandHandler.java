package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.AbandonBotVsBotMatchCommand;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.application.ports.in.AbandonBotVsBotMatchUseCase;
import com.villo.truco.domain.model.match.exceptions.AbandonBotMatchNotOwnerException;
import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchLockingRepository;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class AbandonBotVsBotMatchCommandHandler implements AbandonBotVsBotMatchUseCase {

  private final BotVsBotMatchRegistry botVsBotMatchRegistry;
  private final MatchLockingRepository matchLockingRepository;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;

  public AbandonBotVsBotMatchCommandHandler(final BotVsBotMatchRegistry botVsBotMatchRegistry,
      final MatchLockingRepository matchLockingRepository, final MatchRepository matchRepository,
      final MatchEventNotifier matchEventNotifier) {

    this.botVsBotMatchRegistry = Objects.requireNonNull(botVsBotMatchRegistry);
    this.matchLockingRepository = Objects.requireNonNull(matchLockingRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
  }

  @Override
  public Void handle(final AbandonBotVsBotMatchCommand command) {

    final var owner = this.botVsBotMatchRegistry.findOwnerByMatchId(command.matchId());
    if (owner.isEmpty() || !owner.get().equals(command.ownerId())) {
      throw new AbandonBotMatchNotOwnerException();
    }

    final var match = this.matchLockingRepository.findByIdForUpdate(command.matchId())
        .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

    match.abandon(match.getPlayerOne());

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();

    return null;
  }

}
