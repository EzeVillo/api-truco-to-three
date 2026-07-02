package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.SpectatorCountChanged;
import com.villo.truco.application.usecases.commands.SpectatorCountChangedPublisher;
import com.villo.truco.application.usecases.commands.SpectatorshipLifecycleManager;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.ports.CompetitionMembershipResolver;
import com.villo.truco.domain.ports.FriendshipSpectateEligibilityResolver;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.inmemory.InMemorySpectatorshipRepository;
import com.villo.truco.social.application.services.FriendAvailabilityChangeNotifier;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.events.FriendshipRemovedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpectatorCleanupOnFriendshipRemovedEventHandler")
class SpectatorCleanupOnFriendshipRemovedEventHandlerTest {

  private static Fixture fixture(final CompetitionMembershipResolver competitionResolver,
      final FriendshipSpectateEligibilityResolver friendshipResolver) {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var spectator = PlayerId.generate();
    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    match.startMatch(playerOne);
    match.startMatch(playerTwo);

    final var repository = new InMemorySpectatorshipRepository();
    final var spectatorship = Spectatorship.create(spectator);
    spectatorship.startWatching(match.getId());
    repository.save(spectatorship);

    final var events = new ArrayList<ApplicationEvent>();
    final MatchQueryRepository matchRepo = new SingleMatchRepo(match);
    final var lifecycleManager = new SpectatorshipLifecycleManager(repository,
        new SpectatorCountChangedPublisher(matchRepo, repository, events::add),
        mock(FriendAvailabilityChangeNotifier.class), mock(PresenceNotifier.class));
    final var handler = new SpectatorCleanupOnFriendshipRemovedEventHandler(repository, matchRepo,
        competitionResolver, friendshipResolver, lifecycleManager);

    return new Fixture(playerOne, spectator, match, repository, events, handler);
  }

  private static FriendshipRemovedEvent removedFriendship(final PlayerId requester,
      final PlayerId addressee) {

    final var friendship = Friendship.request(requester, addressee, true);
    friendship.accept(addressee);
    friendship.remove(requester);
    return (FriendshipRemovedEvent) friendship.getFriendshipDomainEvents().getLast();
  }

  @Test
  @DisplayName("corta spectate habilitado solo por amistad removida")
  void stopsSpectateEnabledOnlyByRemovedFriendship() {

    final var fixture = fixture((matchId, playerId) -> false, (match, spectatorId) -> false);

    fixture.handler.handle(removedFriendship(fixture.spectator, fixture.playerOne));

    assertThat(fixture.repository.findBySpectatorId(fixture.spectator)
        .flatMap(Spectatorship::getActiveMatchId)).isEmpty();
    assertThat(fixture.publishedEvents).hasSize(1);
    assertThat(fixture.publishedEvents.getFirst()).isInstanceOf(SpectatorCountChanged.class);
  }

  @Test
  @DisplayName("conserva spectate si sigue habilitado por liga o copa")
  void keepsSpectateWhenCompetitionStillAllowsIt() {

    final var fixture = fixture((matchId, playerId) -> true, (match, spectatorId) -> false);

    fixture.handler.handle(removedFriendship(fixture.spectator, fixture.playerOne));

    assertThat(fixture.repository.findBySpectatorId(fixture.spectator)
        .flatMap(Spectatorship::getActiveMatchId)).contains(fixture.match.getId());
    assertThat(fixture.publishedEvents).isEmpty();
  }

  private record Fixture(PlayerId playerOne, PlayerId spectator, Match match,
                         InMemorySpectatorshipRepository repository,
                         List<ApplicationEvent> publishedEvents,
                         SpectatorCleanupOnFriendshipRemovedEventHandler handler) {

  }

  private record SingleMatchRepo(Match match) implements MatchQueryRepository {

    @Override
    public Optional<Match> findById(final MatchId matchId) {

      return matchId.equals(this.match.getId()) ? Optional.of(this.match) : Optional.empty();
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

      return Optional.empty();
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

    @Override
    public Set<PlayerId> findPlayersWithUnfinishedMatch(final Set<PlayerId> playerIds) {

      return Set.of();
    }

    @Override
    public Map<PlayerId, Match> findUnfinishedByPlayers(final Set<PlayerId> playerIds) {

      return Map.of();
    }

  }

}
