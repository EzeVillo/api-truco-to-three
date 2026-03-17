package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.GuestLoginCommand;
import com.villo.truco.application.dto.GuestLoginDTO;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.application.ports.in.GuestLoginUseCase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class GuestLoginCommandHandler implements GuestLoginUseCase {

  private final PlayerTokenProvider tokenProvider;

  public GuestLoginCommandHandler(final PlayerTokenProvider tokenProvider) {

    this.tokenProvider = Objects.requireNonNull(tokenProvider);
  }

  @Override
  public GuestLoginDTO handle(final GuestLoginCommand command) {

    final var playerId = PlayerId.generate();
    final var accessToken = this.tokenProvider.generateAccessToken(playerId);

    return new GuestLoginDTO(playerId.value().toString(), accessToken);
  }

}
