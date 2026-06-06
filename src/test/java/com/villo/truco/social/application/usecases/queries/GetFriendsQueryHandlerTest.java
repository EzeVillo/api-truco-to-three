package com.villo.truco.social.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.queries.GetFriendsQuery;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.application.services.SocialViewAssembler;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetFriendsQueryHandler")
class GetFriendsQueryHandlerTest {

  private static GetFriendsQueryHandler handler(final PlayerId requester, final PlayerId friend,
      final Friendship friendship,
      final java.util.function.Function<PlayerId, Optional<Match>> matchResolver) {

    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(
        Map.of(requester, "ana", friend, "martina"));

    final var friendshipRepo = mock(FriendshipQueryRepository.class);
    when(friendshipRepo.findAcceptedByPlayer(requester)).thenReturn(List.of(friendship));

    return new GetFriendsQueryHandler(new SocialUserGuard(userRepo), friendshipRepo,
        new MatchRepo(matchResolver), new SocialViewAssembler(userRepo));
  }

  private static Friendship acceptedFriendship(final PlayerId requester, final PlayerId friend) {

    final var friendship = Friendship.request(requester, friend);
    friendship.accept(friend);
    return friendship;
  }

  private static Match startedMatch(final PlayerId playerOne, final PlayerId playerTwo) {

    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    match.startMatch(playerOne);
    match.startMatch(playerTwo);
    return match;
  }

  @Test
  @DisplayName("devuelve spectatableMatch para amigo con partida en progreso")
  void returnsSpectatableMatchForFriendInProgress() {

    final var requester = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var friendship = acceptedFriendship(requester, friend);
    final var match = startedMatch(friend, PlayerId.generate());
    final var handler = handler(requester, friend, friendship, player -> Optional.of(match));

    final var result = handler.handle(new GetFriendsQuery(requester));

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().friendUsername()).isEqualTo("martina");
    assertThat(result.getFirst().spectatableMatch().id()).isEqualTo(
        match.getId().value().toString());
    assertThat(result.getFirst().spectatableMatch().status()).isEqualTo("IN_PROGRESS");
  }

  @Test
  @DisplayName("devuelve spectatableMatch null si el amigo no tiene partida en progreso")
  void returnsNullWhenFriendHasNoInProgressMatch() {

    final var requester = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var friendship = acceptedFriendship(requester, friend);
    final var handler = handler(requester, friend, friendship, player -> Optional.empty());

    final var result = handler.handle(new GetFriendsQuery(requester));

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().friendUsername()).isEqualTo("martina");
    assertThat(result.getFirst().spectatableMatch()).isNull();
  }

  private record MatchRepo(
      java.util.function.Function<PlayerId, Optional<Match>> matchResolver) implements
      MatchQueryRepository {

    @Override
    public Optional<Match> findById(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public boolean hasActiveMatch(final PlayerId playerId) {

      return false;
    }

    @Override
    public boolean hasUnfinishedMatch(final PlayerId playerId) {

      return false;
    }

    @Override
    public Optional<Match> findUnfinishedByPlayer(final PlayerId playerId) {

      return this.matchResolver.apply(playerId);
    }

    @Override
    public List<MatchId> findIdleMatchIds(final Instant idleSince) {

      return List.of();
    }

    @Override
    public List<Match> findPublicWaiting() {

      return List.of();
    }

    @Override
    public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

      return new CursorPageResult<>(List.of(), null);
    }

  }

}
