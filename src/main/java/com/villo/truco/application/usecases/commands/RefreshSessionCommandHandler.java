package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.RefreshSessionCommand;
import com.villo.truco.application.dto.SessionTokenDTO;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.RefreshSessionUseCase;
import java.util.Objects;

public final class RefreshSessionCommandHandler implements RefreshSessionUseCase {

  private final PlayerTokenProvider playerTokenProvider;

  public RefreshSessionCommandHandler(final PlayerTokenProvider playerTokenProvider) {

    this.playerTokenProvider = Objects.requireNonNull(playerTokenProvider);
  }

  @Override
  public SessionTokenDTO handle(final RefreshSessionCommand command) {

    final var identity = this.playerTokenProvider.validateRefreshToken(command.refreshToken());

    final var accessToken = this.playerTokenProvider.generateAccessToken(identity.matchId(),
        identity.playerId());

    final var refreshToken = this.playerTokenProvider.generateRefreshToken(identity.matchId(),
        identity.playerId());

    return new SessionTokenDTO(accessToken, refreshToken);
  }

}
