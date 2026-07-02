package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.RetryableTransactionalRunner;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.MatchTimeoutEntry;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TimeoutIdleMatchesCommandHandler")
class TimeoutIdleMatchesCommandHandlerTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
  }

  private Match waitingMatch() {

    return Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5), true),
        Visibility.PRIVATE);
  }

  private Match readyMatch() {

    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(5), true));
    match.clearDomainEvents();
    return match;
  }

  private Match inProgressMatch() {

    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(5), true));
    match.startMatch(playerOne);
    match.startMatch(playerTwo);
    match.clearDomainEvents();
    return match;
  }

  private TimeoutIdleMatchesCommandHandler handlerFor(final Map<MatchId, Match> matches,
      final AtomicReference<Match> savedMatch, final List<MatchDomainEvent> publishedEvents) {

    final MatchQueryRepository queryRepo = new MatchQueryRepository() {

      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.ofNullable(matches.get(matchId));
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
    };

    final MatchRepository matchRepository = new MatchRepository() {
      @Override
      public void save(final Match match) {

        savedMatch.set(match);
      }

      @Override
      public Stream<MatchTimeoutEntry> findActiveWithTimeoutDeadline() {

        return Stream.empty();
      }
    };
    final MatchEventNotifier notifier = publishedEvents::addAll;
    final RetryableTransactionalRunner transactionalRunner = Runnable::run;

    return new TimeoutIdleMatchesCommandHandler(queryRepo, matchRepository, notifier,
        transactionalRunner);
  }

  @Test
  @DisplayName("match en WAITING_FOR_PLAYERS se cancela y queda CANCELLED")
  void waitingMatchIsCancelled() {

    final var match = waitingMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle(match.getId());

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getStatus()).isEqualTo(MatchStatus.CANCELLED);
  }

  @Test
  @DisplayName("match en WAITING_FOR_PLAYERS emite MatchCancelledEvent")
  void waitingMatchPublishesMatchCancelledEvent() {

    final var match = waitingMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle(match.getId());

    assertThat(publishedEvents).anyMatch(e -> e instanceof MatchCancelledEvent);
  }

  @Test
  @DisplayName("match en WAITING_FOR_PLAYERS limpia eventos tras publicar")
  void waitingMatchClearsDomainEventsAfterPublishing() {

    final var match = waitingMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle(match.getId());

    assertThat(match.getDomainEvents()).isEmpty();
  }

  @Test
  @DisplayName("match en READY se forfeitea y queda FINISHED")
  void readyMatchIsForfeited() {

    final var match = readyMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle(match.getId());

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getStatus()).isEqualTo(MatchStatus.FINISHED);
    assertThat(publishedEvents).anyMatch(e -> e instanceof MatchForfeitedEvent);
  }

  @Test
  @DisplayName("match en IN_PROGRESS se forfeitea y queda FINISHED")
  void inProgressMatchIsForfeited() {

    final var match = inProgressMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle(match.getId());

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getStatus()).isEqualTo(MatchStatus.FINISHED);
    assertThat(publishedEvents).anyMatch(e -> e instanceof MatchForfeitedEvent);
  }

  @Test
  @DisplayName("match inexistente es ignorado")
  void unknownMatchIdIsIgnored() {

    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = handlerFor(Map.of(), savedMatch, publishedEvents);

    handler.handle(MatchId.generate());

    assertThat(savedMatch.get()).isNull();
    assertThat(publishedEvents).isEmpty();
  }

  @Test
  @DisplayName("match ya FINISHED es ignorado")
  void finishedMatchIsSkipped() {

    final var match = inProgressMatch();
    match.abandon(playerTwo);
    match.clearDomainEvents();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<MatchDomainEvent>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle(match.getId());

    assertThat(savedMatch.get()).isNull();
    assertThat(publishedEvents).isEmpty();
  }

}
