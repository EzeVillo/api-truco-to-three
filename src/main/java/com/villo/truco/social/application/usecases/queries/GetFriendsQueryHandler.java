package com.villo.truco.social.application.usecases.queries;

import com.villo.truco.social.application.dto.FriendSummaryDTO;
import com.villo.truco.social.application.ports.in.GetFriendsUseCase;
import com.villo.truco.social.application.queries.GetFriendsQuery;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.util.List;
import java.util.Objects;

public final class GetFriendsQueryHandler implements GetFriendsUseCase {

  private final SocialUserGuard socialUserGuard;
  private final FriendshipQueryRepository friendshipQueryRepository;
  private final SocialViewAssembler socialViewAssembler;

  public GetFriendsQueryHandler(final SocialUserGuard socialUserGuard,
      final FriendshipQueryRepository friendshipQueryRepository,
      final SocialViewAssembler socialViewAssembler) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.socialViewAssembler = Objects.requireNonNull(socialViewAssembler);
  }

  @Override
  public List<FriendSummaryDTO> handle(final GetFriendsQuery query) {

    this.socialUserGuard.ensureRegisteredUser(query.playerId());
    return this.friendshipQueryRepository.findAcceptedByPlayer(query.playerId()).stream().map(
            friendship -> this.socialViewAssembler.toFriendSummaryDto(friendship, query.playerId()))
        .toList();
  }

}
