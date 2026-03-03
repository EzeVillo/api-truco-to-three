package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.ExchangeSessionGrantCommand;
import com.villo.truco.application.dto.SessionTokenDTO;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.SessionGrantProvider;
import com.villo.truco.application.ports.in.ExchangeSessionGrantUseCase;
import java.util.Objects;

public final class ExchangeSessionGrantCommandHandler implements ExchangeSessionGrantUseCase {

  private final SessionGrantProvider sessionGrantProvider;
  private final PlayerTokenProvider playerTokenProvider;

  public ExchangeSessionGrantCommandHandler(final SessionGrantProvider sessionGrantProvider,
      final PlayerTokenProvider playerTokenProvider) {

    this.sessionGrantProvider = Objects.requireNonNull(sessionGrantProvider);
    this.playerTokenProvider = Objects.requireNonNull(playerTokenProvider);
  }

  @Override
  public SessionTokenDTO handle(final ExchangeSessionGrantCommand command) {

    final var identity = this.sessionGrantProvider.validateAndConsumeGrant(command.sessionGrant());

    final var accessToken = this.playerTokenProvider.generateAccessToken(identity.matchId(),
        identity.playerId());

    final var refreshToken = this.playerTokenProvider.generateRefreshToken(identity.matchId(),
        identity.playerId());

    return new SessionTokenDTO(accessToken, refreshToken);
  }

}
