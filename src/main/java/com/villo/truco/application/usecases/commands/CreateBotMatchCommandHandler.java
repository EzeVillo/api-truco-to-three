package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateBotMatchCommand;
import com.villo.truco.application.dto.CreateBotMatchDTO;
import com.villo.truco.application.exceptions.BotNotFoundException;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.CreateBotMatchUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class CreateBotMatchCommandHandler implements CreateBotMatchUseCase {

  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final BotRegistry botRegistry;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public CreateBotMatchCommandHandler(final MatchRepository matchRepository,
      final MatchEventNotifier matchEventNotifier, final BotRegistry botRegistry,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public CreateBotMatchDTO handle(final CreateBotMatchCommand command) {

    if (this.botRegistry.getProfile(command.botPlayerId()).isEmpty()) {
      throw new BotNotFoundException(command.botPlayerId());
    }

    this.playerAvailabilityChecker.ensureAvailable(command.humanPlayerId());

    final var rules = MatchRules.fromGamesToPlay(command.gamesToPlay());
    final var match = Match.createReady(command.humanPlayerId(), command.botPlayerId(), rules);

    match.startMatch(command.humanPlayerId());
    match.startMatch(command.botPlayerId());

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();

    return new CreateBotMatchDTO(match.getId().value().toString());
  }

}
