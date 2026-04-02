package com.villo.truco.auth.application.usecases.commands;

import com.villo.truco.auth.application.commands.GuestLoginCommand;
import com.villo.truco.auth.application.model.GuestAuthenticatedSession;
import com.villo.truco.auth.application.ports.in.GuestLoginUseCase;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class GuestLoginCommandHandler implements GuestLoginUseCase {

  private final AccessTokenIssuer accessTokenIssuer;

  public GuestLoginCommandHandler(final AccessTokenIssuer accessTokenIssuer) {

    this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
  }

  @Override
  public GuestAuthenticatedSession handle(final GuestLoginCommand command) {

    final var playerId = PlayerId.generate();
    final var accessToken = this.accessTokenIssuer.issueForGuest(playerId);

    return new GuestAuthenticatedSession(playerId, accessToken.value(), accessToken.expiresIn());
  }

}
