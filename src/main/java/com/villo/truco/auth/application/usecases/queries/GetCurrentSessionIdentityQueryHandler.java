package com.villo.truco.auth.application.usecases.queries;

import com.villo.truco.application.exceptions.UnauthorizedAccessException;
import com.villo.truco.auth.application.model.AuthenticatedSessionIdentity;
import com.villo.truco.auth.application.ports.in.GetCurrentSessionIdentityUseCase;
import com.villo.truco.auth.application.queries.GetCurrentSessionIdentityQuery;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import java.util.Objects;

public final class GetCurrentSessionIdentityQueryHandler implements
    GetCurrentSessionIdentityUseCase {

  private static final String USER_TOKEN_USE = "user";
  private static final String GUEST_TOKEN_USE = "guest";

  private final UserQueryRepository userQueryRepository;

  public GetCurrentSessionIdentityQueryHandler(final UserQueryRepository userQueryRepository) {

    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
  }

  @Override
  public AuthenticatedSessionIdentity handle(final GetCurrentSessionIdentityQuery query) {

    if (GUEST_TOKEN_USE.equals(query.tokenUse())) {
      return new AuthenticatedSessionIdentity(query.playerId(), null, GUEST_TOKEN_USE);
    }

    if (!USER_TOKEN_USE.equals(query.tokenUse())) {
      throw new UnauthorizedAccessException("Invalid token use");
    }

    final var username = this.userQueryRepository.findUsernameById(query.playerId())
        .orElseThrow(() -> new UnauthorizedAccessException("Authenticated user not found"));
    return new AuthenticatedSessionIdentity(query.playerId(), username, USER_TOKEN_USE);
  }

}
