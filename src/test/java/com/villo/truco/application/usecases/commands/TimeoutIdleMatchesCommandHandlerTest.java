package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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

    return Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
  }

  private Match readyMatch() {

    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
    match.clearDomainEvents();
    return match;
  }

  private Match inProgressMatch() {

    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
    match.startMatch(playerOne);
    match.startMatch(playerTwo);
    match.clearDomainEvents();
    return match;
  }

  private TimeoutIdleMatchesCommandHandler handlerFor(final Map<MatchId, Match> matches,
      final AtomicReference<Match> savedMatch, final List<DomainEventBase> publishedEvents) {

    final MatchQueryRepository queryRepo = new MatchQueryRepository() {

      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.ofNullable(matches.get(matchId));
      }

      @Override
      public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public boolean hasActiveMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public List<MatchId> findIdleMatchIds(final Instant idleSince) {

        return List.copyOf(matches.keySet());
      }
    };

    final MatchRepository matchRepository = savedMatch::set;
    final MatchEventNotifier notifier = (matchId, p1, p2, events) -> publishedEvents.addAll(events);
    final TransactionalRunner transactionalRunner = Runnable::run;

    return new TimeoutIdleMatchesCommandHandler(queryRepo, matchRepository, notifier,
        transactionalRunner, Duration.ofMinutes(10));
  }

  @Test
  @DisplayName("match en WAITING_FOR_PLAYERS se cancela y queda FINISHED")
  void waitingMatchIsCancelled() {

    final var match = waitingMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle();

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getStatus()).isEqualTo(MatchStatus.FINISHED);
  }

  @Test
  @DisplayName("match en WAITING_FOR_PLAYERS emite MatchCancelledEvent")
  void waitingMatchPublishesMatchCancelledEvent() {

    final var match = waitingMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle();

    assertThat(publishedEvents).anyMatch(e -> e instanceof MatchCancelledEvent);
  }

  @Test
  @DisplayName("match en WAITING_FOR_PLAYERS limpia eventos tras publicar")
  void waitingMatchClearsDomainEventsAfterPublishing() {

    final var match = waitingMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle();

    assertThat(match.getDomainEvents()).isEmpty();
  }

  @Test
  @DisplayName("match en READY se forfeitea y queda FINISHED")
  void readyMatchIsForfeited() {

    final var match = readyMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle();

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getStatus()).isEqualTo(MatchStatus.FINISHED);
    assertThat(publishedEvents).anyMatch(e -> e instanceof MatchForfeitedEvent);
  }

  @Test
  @DisplayName("match en IN_PROGRESS se forfeitea y queda FINISHED")
  void inProgressMatchIsForfeited() {

    final var match = inProgressMatch();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle();

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getStatus()).isEqualTo(MatchStatus.FINISHED);
    assertThat(publishedEvents).anyMatch(e -> e instanceof MatchForfeitedEvent);
  }

  @Test
  @DisplayName("match ya FINISHED es ignorado")
  void finishedMatchIsSkipped() {

    final var match = inProgressMatch();
    match.forfeit(playerOne);
    match.clearDomainEvents();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerFor(Map.of(match.getId(), match), savedMatch, publishedEvents);

    handler.handle();

    assertThat(savedMatch.get()).isNull();
    assertThat(publishedEvents).isEmpty();
  }

  @Test
  @DisplayName("lista vacía de idle matches no hace nada")
  void emptyIdleListDoesNothing() {

    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerFor(Map.of(), savedMatch, publishedEvents);

    handler.handle();

    assertThat(savedMatch.get()).isNull();
    assertThat(publishedEvents).isEmpty();
  }

}
