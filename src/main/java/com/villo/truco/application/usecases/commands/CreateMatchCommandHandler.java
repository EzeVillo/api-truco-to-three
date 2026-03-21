package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.dto.CreateMatchDTO;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class CreateMatchCommandHandler implements CreateMatchUseCase {

  private final MatchRepository matchRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public CreateMatchCommandHandler(final MatchRepository matchRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public CreateMatchDTO handle(final CreateMatchCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var rules = MatchRules.fromGamesToPlay(command.gamesToPlay());
    final var match = Match.create(command.playerId(), rules);

    this.matchRepository.save(match);

    return new CreateMatchDTO(match.getId().value().toString(), match.getInviteCode().value());
  }

}
