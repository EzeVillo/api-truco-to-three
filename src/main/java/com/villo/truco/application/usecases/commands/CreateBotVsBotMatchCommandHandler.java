package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateBotVsBotMatchCommand;
import com.villo.truco.application.dto.CreateBotVsBotMatchDTO;
import com.villo.truco.application.exceptions.BotNotFoundException;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.CreateBotVsBotMatchUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.SamePlayerMatchException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class CreateBotVsBotMatchCommandHandler implements CreateBotVsBotMatchUseCase {

  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final BotRegistry botRegistry;
  private final BotVsBotMatchRegistry botVsBotMatchRegistry;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public CreateBotVsBotMatchCommandHandler(final MatchRepository matchRepository,
      final MatchEventNotifier matchEventNotifier, final BotRegistry botRegistry,
      final BotVsBotMatchRegistry botVsBotMatchRegistry,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.botVsBotMatchRegistry = Objects.requireNonNull(botVsBotMatchRegistry);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public CreateBotVsBotMatchDTO handle(final CreateBotVsBotMatchCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.ownerId());

    if (command.botOneId().equals(command.botTwoId())) {
      throw new SamePlayerMatchException();
    }
    if (this.botRegistry.getProfile(command.botOneId()).isEmpty()) {
      throw new BotNotFoundException(command.botOneId());
    }
    if (this.botRegistry.getProfile(command.botTwoId()).isEmpty()) {
      throw new BotNotFoundException(command.botTwoId());
    }

    final var rules = MatchRules.fromGamesToPlay(command.gamesToPlay(), false);
    final var match = Match.createReady(command.botOneId(), command.botTwoId(), rules);

    match.startMatch(command.botOneId());
    match.startMatch(command.botTwoId());

    this.matchRepository.save(match);
    this.botVsBotMatchRegistry.register(match.getId(), command.ownerId());
    this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();

    return new CreateBotVsBotMatchDTO(match.getId().value().toString());
  }

}
