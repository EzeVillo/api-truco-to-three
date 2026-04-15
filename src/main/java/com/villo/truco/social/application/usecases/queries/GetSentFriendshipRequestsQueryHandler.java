package com.villo.truco.social.application.usecases.queries;

import com.villo.truco.social.application.dto.OutgoingFriendshipRequestDTO;
import com.villo.truco.social.application.ports.in.GetSentFriendshipRequestsUseCase;
import com.villo.truco.social.application.queries.GetSentFriendshipRequestsQuery;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.util.List;
import java.util.Objects;

public final class GetSentFriendshipRequestsQueryHandler implements
    GetSentFriendshipRequestsUseCase {

  private final SocialUserGuard socialUserGuard;
  private final FriendshipQueryRepository friendshipQueryRepository;
  private final SocialViewAssembler socialViewAssembler;

  public GetSentFriendshipRequestsQueryHandler(final SocialUserGuard socialUserGuard,
      final FriendshipQueryRepository friendshipQueryRepository,
      final SocialViewAssembler socialViewAssembler) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.socialViewAssembler = Objects.requireNonNull(socialViewAssembler);
  }

  @Override
  public List<OutgoingFriendshipRequestDTO> handle(final GetSentFriendshipRequestsQuery query) {

    this.socialUserGuard.ensureRegisteredUser(query.requesterId());
    return this.friendshipQueryRepository.findPendingSentBy(query.requesterId()).stream()
        .map(this.socialViewAssembler::toOutgoingFriendshipRequestDto).toList();
  }

}
