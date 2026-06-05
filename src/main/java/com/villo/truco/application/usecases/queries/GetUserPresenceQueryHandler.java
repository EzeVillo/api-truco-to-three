package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.UserPresenceDTO;
import com.villo.truco.application.ports.in.GetUserPresenceUseCase;
import com.villo.truco.application.queries.GetUserPresenceQuery;
import java.util.Objects;

public final class GetUserPresenceQueryHandler implements GetUserPresenceUseCase {

  private final UserPresenceResolver userPresenceResolver;

  public GetUserPresenceQueryHandler(final UserPresenceResolver userPresenceResolver) {

    this.userPresenceResolver = Objects.requireNonNull(userPresenceResolver);
  }

  @Override
  public UserPresenceDTO handle(final GetUserPresenceQuery query) {

    return this.userPresenceResolver.resolve(query.requester());
  }

}
