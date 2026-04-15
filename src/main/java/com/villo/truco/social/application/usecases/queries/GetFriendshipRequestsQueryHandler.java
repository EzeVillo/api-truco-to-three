package com.villo.truco.social.application.usecases.queries;

import com.villo.truco.social.application.dto.IncomingFriendshipRequestDTO;
import com.villo.truco.social.application.ports.in.GetFriendshipRequestsUseCase;
import com.villo.truco.social.application.queries.GetFriendshipRequestsQuery;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.util.List;
import java.util.Objects;

public final class GetFriendshipRequestsQueryHandler implements GetFriendshipRequestsUseCase {

  private final SocialUserGuard socialUserGuard;
  private final FriendshipQueryRepository friendshipQueryRepository;
  private final SocialViewAssembler socialViewAssembler;

  public GetFriendshipRequestsQueryHandler(final SocialUserGuard socialUserGuard,
      final FriendshipQueryRepository friendshipQueryRepository,
      final SocialViewAssembler socialViewAssembler) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.socialViewAssembler = Objects.requireNonNull(socialViewAssembler);
  }

  @Override
  public List<IncomingFriendshipRequestDTO> handle(final GetFriendshipRequestsQuery query) {

    this.socialUserGuard.ensureRegisteredUser(query.playerId());
    return this.friendshipQueryRepository.findPendingReceivedBy(query.playerId()).stream()
        .map(this.socialViewAssembler::toIncomingFriendshipRequestDto).toList();
  }

}
