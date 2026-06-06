package com.villo.truco.social.application.usecases.queries;

import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendSummaryDTO;
import com.villo.truco.social.application.dto.SpectatableMatchRefDTO;
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
  private final MatchQueryRepository matchQueryRepository;
  private final SocialViewAssembler socialViewAssembler;

  public GetFriendsQueryHandler(final SocialUserGuard socialUserGuard,
      final FriendshipQueryRepository friendshipQueryRepository,
      final MatchQueryRepository matchQueryRepository,
      final SocialViewAssembler socialViewAssembler) {

    this.socialUserGuard = Objects.requireNonNull(socialUserGuard);
    this.friendshipQueryRepository = Objects.requireNonNull(friendshipQueryRepository);
    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.socialViewAssembler = Objects.requireNonNull(socialViewAssembler);
  }

  @Override
  public List<FriendSummaryDTO> handle(final GetFriendsQuery query) {

    this.socialUserGuard.ensureRegisteredUser(query.playerId());
    return this.friendshipQueryRepository.findAcceptedByPlayer(query.playerId()).stream().map(
        friendship -> this.socialViewAssembler.toFriendSummaryDto(friendship, query.playerId(),
            this.findSpectatableMatch(friendship.counterpartOf(query.playerId())))).toList();
  }

  private SpectatableMatchRefDTO findSpectatableMatch(final PlayerId friendId) {

    return this.matchQueryRepository.findUnfinishedByPlayer(friendId)
        .filter(match -> match.getStatus() == MatchStatus.IN_PROGRESS).map(
            match -> new SpectatableMatchRefDTO(match.getId().value().toString(),
                match.getStatus().name())).orElse(null);
  }

}
