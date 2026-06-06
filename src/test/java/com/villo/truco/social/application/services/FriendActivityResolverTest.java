package com.villo.truco.social.application.services;

import static com.villo.truco.social.application.services.FriendActivityTestFixtures.acceptedFriendship;
import static com.villo.truco.social.application.services.FriendActivityTestFixtures.startedMatch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FriendActivityResolver")
class FriendActivityResolverTest {

  private static FriendActivityResolver resolver(final PlayerId viewer,
      final List<Friendship> friendships, final Map<PlayerId, String> usernames,
      final java.util.function.Function<PlayerId, Optional<Match>> matchResolver) {

    final var friendshipRepository = mock(FriendshipQueryRepository.class);
    when(friendshipRepository.findAcceptedByPlayer(viewer)).thenReturn(friendships);
    for (final var friendship : friendships) {
      when(friendshipRepository.findAcceptedByPlayer(friendship.counterpartOf(viewer))).thenReturn(
          List.of(friendship));
    }

    final var userRepository = mock(UserQueryRepository.class);
    when(userRepository.findUsernamesByIds(anySet())).thenAnswer(invocation -> {
      final java.util.Set<PlayerId> ids = invocation.getArgument(0);
      return usernames.entrySet().stream().filter(entry -> ids.contains(entry.getKey()))
          .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    });

    return new FriendActivityResolver(friendshipRepository, new MatchRepo(matchResolver),
        userRepository);
  }

  @Test
  @DisplayName("arma snapshot con spectatableMatch para amigo en partida en progreso")
  void resolvesStateWithSpectatableMatch() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var rival = PlayerId.generate();
    final var match = startedMatch(friend, rival);
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(viewer, "ana", friend, "martina"), player -> Optional.of(match));

    final var state = resolver.resolveState(viewer);

    assertThat(state.friends()).hasSize(1);
    assertThat(state.friends().getFirst().friendUsername()).isEqualTo("martina");
    assertThat(state.friends().getFirst().spectatableMatch().id()).isEqualTo(
        match.getId().value().toString());
    assertThat(state.friends().getFirst().spectatableMatch().status()).isEqualTo("IN_PROGRESS");
  }

  @Test
  @DisplayName("arma snapshot con spectatableMatch null si el amigo no juega")
  void resolvesStateWithoutSpectatableMatch() {

    final var viewer = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, friend)),
        Map.of(viewer, "ana", friend, "martina"), player -> Optional.empty());

    final var state = resolver.resolveState(viewer);

    assertThat(state.friends()).singleElement().satisfies(activity -> {
      assertThat(activity.friendUsername()).isEqualTo("martina");
      assertThat(activity.spectatableMatch()).isNull();
    });
  }

  @Test
  @DisplayName("no crea cambios para usuarios sin amistad aceptada")
  void ignoresPlayersWithoutAcceptedFriends() {

    final var player = PlayerId.generate();
    final var rival = PlayerId.generate();
    final var match = startedMatch(player, rival);
    final var resolver = resolver(player, List.of(), Map.of(player, "martina"),
        ignored -> Optional.empty());

    final var changes = resolver.resolveMatchActivityChangesByRecipient(match.getId(), player,
        rival, player, true);

    assertThat(changes).isEmpty();
  }

  @Test
  @DisplayName("publica cambio null cuando la partida deja de ser espectable")
  void resolvesNullChangeWhenMatchStops() {

    final var activePlayer = PlayerId.generate();
    final var viewer = PlayerId.generate();
    final var rival = PlayerId.generate();
    final var match = startedMatch(activePlayer, rival);
    final var resolver = resolver(viewer, List.of(acceptedFriendship(viewer, activePlayer)),
        Map.of(activePlayer, "martina", viewer, "ana"), ignored -> Optional.empty());

    final var changes = resolver.resolveMatchActivityChangesByRecipient(match.getId(), activePlayer,
        rival, activePlayer, false);

    assertThat(changes).containsOnlyKeys(viewer);
    assertThat(changes.get(viewer).friendUsername()).isEqualTo("martina");
    assertThat(changes.get(viewer).spectatableMatch()).isNull();
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
