package com.villo.truco.social.application.usecases.queries;

import com.villo.truco.social.application.dto.FriendActivityStateDTO;
import com.villo.truco.social.application.ports.in.GetFriendActivityUseCase;
import com.villo.truco.social.application.queries.GetFriendActivityQuery;
import com.villo.truco.social.application.services.FriendActivityResolver;
import com.villo.truco.social.application.services.SocialUserGuard;
import java.util.Objects;

public final class GetFriendActivityQueryHandler implements GetFriendActivityUseCase {

  private final SocialUserGuard socialUserGuard;
  private final FriendActivityResolver friendActivityResolver;

  public GetFriendActivityQueryHandler(final SocialUserGuard socialUserGuard,
      final FriendActivityResolver friendActivityResolver) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.friendActivityResolver = Objects.requireNonNull(friendActivityResolver);
  }

  @Override
  public FriendActivityStateDTO handle(final GetFriendActivityQuery query) {

    this.socialUserGuard.ensureRegisteredUser(query.playerId());
    return this.friendActivityResolver.resolveState(query.playerId());
  }

}
