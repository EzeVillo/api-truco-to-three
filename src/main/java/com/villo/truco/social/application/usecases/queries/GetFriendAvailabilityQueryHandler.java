package com.villo.truco.social.application.usecases.queries;

import com.villo.truco.social.application.dto.FriendAvailabilityStateDTO;
import com.villo.truco.social.application.ports.in.GetFriendAvailabilityUseCase;
import com.villo.truco.social.application.queries.GetFriendAvailabilityQuery;
import com.villo.truco.social.application.services.FriendAvailabilityResolver;
import com.villo.truco.social.application.services.SocialUserGuard;
import java.util.Objects;

public final class GetFriendAvailabilityQueryHandler implements GetFriendAvailabilityUseCase {

  private final SocialUserGuard socialUserGuard;
  private final FriendAvailabilityResolver friendAvailabilityResolver;

  public GetFriendAvailabilityQueryHandler(final SocialUserGuard socialUserGuard,
      final FriendAvailabilityResolver friendAvailabilityResolver) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.friendAvailabilityResolver = Objects.requireNonNull(friendAvailabilityResolver);
  }

  @Override
  public FriendAvailabilityStateDTO handle(final GetFriendAvailabilityQuery query) {

    this.socialUserGuard.ensureRegisteredUser(query.playerId());
    return this.friendAvailabilityResolver.resolveState(query.playerId());
  }

}
