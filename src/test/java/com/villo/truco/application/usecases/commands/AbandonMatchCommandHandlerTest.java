package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.AbandonMatchCommand;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.exceptions.PlayerNotInMatchException;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbandonMatchCommandHandler")
class AbandonMatchCommandHandlerTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
  }

  private Match matchInProgress() {

    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
    match.startMatch(playerOne);
    match.startMatch(playerTwo);
    match.clearDomainEvents();
    return match;
  }

  private AbandonMatchCommandHandler handlerWith(final Match match,
      final AtomicReference<Match> savedMatch, final List<DomainEventBase> publishedEvents) {

    final MatchQueryRepository queryRepo = new MatchQueryRepository() {

      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.of(match);
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

        return List.of();
      }
    };

    final MatchRepository matchRepository = savedMatch::set;

    final MatchEventNotifier notifier = (matchId, p1, p2, events) -> publishedEvents.addAll(events);

    final var resolver = new MatchResolver(queryRepo);
    return new AbandonMatchCommandHandler(resolver, matchRepository, notifier);
  }

  @Test
  @DisplayName("playerOne abandona → playerTwo gana, partida FINISHED")
  void playerOneAbandonsMakesPlayerTwoWinner() {

    final var match = matchInProgress();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerWith(match, savedMatch, publishedEvents);

    handler.handle(
        new AbandonMatchCommand(match.getId().value().toString(), playerOne.value().toString()));

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getStatus()).isEqualTo(MatchStatus.FINISHED);
    assertThat(savedMatch.get().getMatchWinner()).isEqualTo(playerTwo);
  }

  @Test
  @DisplayName("playerTwo abandona → playerOne gana, partida FINISHED")
  void playerTwoAbandonsMakesPlayerOneWinner() {

    final var match = matchInProgress();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerWith(match, savedMatch, publishedEvents);

    handler.handle(
        new AbandonMatchCommand(match.getId().value().toString(), playerTwo.value().toString()));

    assertThat(savedMatch.get().getStatus()).isEqualTo(MatchStatus.FINISHED);
    assertThat(savedMatch.get().getMatchWinner()).isEqualTo(playerOne);
  }

  @Test
  @DisplayName("publica MatchForfeitedEvent tras el abandono")
  void publishesMatchForfeitedEvent() {

    final var match = matchInProgress();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerWith(match, savedMatch, publishedEvents);

    handler.handle(
        new AbandonMatchCommand(match.getId().value().toString(), playerOne.value().toString()));

    assertThat(publishedEvents).anyMatch(e -> e instanceof MatchForfeitedEvent);
  }

  @Test
  @DisplayName("limpia los eventos del aggregate tras publicar")
  void clearsDomainEventsAfterPublishing() {

    final var match = matchInProgress();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerWith(match, savedMatch, publishedEvents);

    handler.handle(
        new AbandonMatchCommand(match.getId().value().toString(), playerOne.value().toString()));

    assertThat(match.getDomainEvents()).isEmpty();
  }

  @Test
  @DisplayName("jugador ajeno al match lanza PlayerNotInMatchException")
  void strangerAbandonThrowsPlayerNotInMatchException() {

    final var match = matchInProgress();
    final var savedMatch = new AtomicReference<Match>();
    final var publishedEvents = new ArrayList<DomainEventBase>();
    final var handler = handlerWith(match, savedMatch, publishedEvents);
    final var stranger = PlayerId.generate();

    assertThatThrownBy(() -> handler.handle(
        new AbandonMatchCommand(match.getId().value().toString(),
            stranger.value().toString()))).isInstanceOf(PlayerNotInMatchException.class);

    assertThat(savedMatch.get()).isNull();
    assertThat(publishedEvents).isEmpty();
  }

}
