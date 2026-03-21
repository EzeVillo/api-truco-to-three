package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinCupCommand;
import com.villo.truco.application.dto.JoinCupDTO;
import com.villo.truco.application.ports.in.JoinCupUseCase;
import com.villo.truco.domain.ports.CupRepository;
import java.util.Objects;

public final class JoinCupCommandHandler implements JoinCupUseCase {

  private final CupResolver cupResolver;
  private final CupRepository cupRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public JoinCupCommandHandler(final CupResolver cupResolver, final CupRepository cupRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.cupResolver = Objects.requireNonNull(cupResolver);
    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  @Override
  public JoinCupDTO handle(final JoinCupCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var cup = this.cupResolver.resolve(command.inviteCode());

    cup.join(command.playerId(), command.inviteCode());

    this.cupRepository.save(cup);

    return new JoinCupDTO(cup.getId().value().toString());
  }

}
