package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.dto.CreateMatchDTO;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class CreateMatchCommandHandler implements CreateMatchUseCase {

  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public CreateMatchCommandHandler(final MatchRepository matchRepository,
      final MatchEventNotifier matchEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public CreateMatchDTO handle(final CreateMatchCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var rules = MatchRules.fromGamesToPlay(command.gamesToPlay());
    final var match = Match.create(command.playerId(), rules, command.visibility());

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();

    return new CreateMatchDTO(match.getId().value().toString(),
        match.getInviteCode() != null ? match.getInviteCode().value() : null,
        match.getVisibility().name());
  }

}
