package com.villo.truco.social.application.usecases.queries;

import com.villo.truco.social.application.dto.FriendSummaryDTO;
import com.villo.truco.social.application.ports.in.GetFriendsUseCase;
import com.villo.truco.social.application.queries.GetFriendsQuery;
import com.villo.truco.social.application.services.FriendAvailabilityResolver;
import com.villo.truco.social.application.services.SocialUserGuard;
import java.util.List;
import java.util.Objects;

public final class GetFriendsQueryHandler implements GetFriendsUseCase {

  private final SocialUserGuard socialUserGuard;
  private final FriendAvailabilityResolver friendAvailabilityResolver;

  public GetFriendsQueryHandler(final SocialUserGuard socialUserGuard,
      final FriendAvailabilityResolver friendAvailabilityResolver) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.friendAvailabilityResolver = Objects.requireNonNull(friendAvailabilityResolver);
  }

  @Override
  public List<FriendSummaryDTO> handle(final GetFriendsQuery query) {

    this.socialUserGuard.ensureRegisteredUser(query.playerId());
    return this.friendAvailabilityResolver.resolveState(query.playerId()).friends().stream().map(
        friend -> new FriendSummaryDTO(friend.friendUsername(), friend.online(),
            friend.availability(), friend.busyReason(), friend.spectatableMatch())).toList();
  }

}
