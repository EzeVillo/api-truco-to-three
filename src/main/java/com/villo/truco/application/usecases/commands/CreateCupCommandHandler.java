package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.CreateCupCommand;
import com.villo.truco.application.dto.CreateCupDTO;
import com.villo.truco.application.ports.in.CreateCupUseCase;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.ports.CupRepository;
import java.util.Objects;

public final class CreateCupCommandHandler implements CreateCupUseCase {

  private final CupRepository cupRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public CreateCupCommandHandler(final CupRepository cupRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public CreateCupDTO handle(final CreateCupCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var cup = Cup.create(command.playerId(), command.numberOfPlayers(),
        command.gamesToPlay());

    this.cupRepository.save(cup);

    return new CreateCupDTO(cup.getId().value().toString(), cup.getInviteCode().value());
  }

}
