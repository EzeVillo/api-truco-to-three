package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinCupCommand;
import com.villo.truco.application.dto.JoinCupDTO;
import com.villo.truco.application.ports.in.JoinCupUseCase;
import com.villo.truco.domain.ports.CupRepository;
import java.util.Objects;

public final class JoinCupCommandHandler implements JoinCupUseCase {

  private final CupResolver cupResolver;
  private final CupRepository cupRepository;

  public JoinCupCommandHandler(final CupResolver cupResolver, final CupRepository cupRepository) {

    this.cupResolver = Objects.requireNonNull(cupResolver);
    this.cupRepository = Objects.requireNonNull(cupRepository);
  }

  @Override
  public JoinCupDTO handle(final JoinCupCommand command) {

    final var cup = this.cupResolver.resolve(command.inviteCode());

    cup.join(command.playerId(), command.inviteCode());

    this.cupRepository.save(cup);

    return new JoinCupDTO(cup.getId().value().toString());
  }

}
