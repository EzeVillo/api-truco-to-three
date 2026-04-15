package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.SpectatorCountChanged;
import com.villo.truco.application.usecases.commands.SpectatorCountChangedPublisher;
import com.villo.truco.application.usecases.commands.SpectatorshipLifecycleManager;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.events.TurnChangedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.inmemory.InMemorySpectatorshipRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpectatorCleanupOnMatchEndEventHandler")
class SpectatorCleanupOnMatchEndEventHandlerTest {

  private MatchId matchId;
  private PlayerId spectator;
  private InMemorySpectatorshipRepository repository;
  private List<ApplicationEvent> publishedEvents;
  private SpectatorCleanupOnMatchEndEventHandler handler;

  @BeforeEach
  void setUp() {

    this.matchId = MatchId.generate();
    this.spectator = PlayerId.generate();
    this.repository = new InMemorySpectatorshipRepository();
    this.publishedEvents = new ArrayList<>();

    final var spectatorship = Spectatorship.create(this.spectator);
    spectatorship.startWatching(this.matchId);
    this.repository.save(spectatorship);

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var match = Match.createReady(p1, p2, MatchRules.fromGamesToPlay(GamesToPlay.of(3)));

    final MatchQueryRepository matchQueryRepository = new MatchQueryRepository() {
      @Override
      public Optional<Match> findById(final MatchId id) {

        return id.equals(matchId) ? Optional.of(match) : Optional.empty();
      }

      @Override
      public Optional<Match> findByJoinCode(final JoinCode joinCode) {

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
      public List<MatchId> findIdleMatchIds(final Instant idleSince) {

        return List.of();
      }

      @Override
      public List<Match> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };

    final var lifecycleManager = new SpectatorshipLifecycleManager(this.repository,
        new SpectatorCountChangedPublisher(matchQueryRepository, this.repository,
            this.publishedEvents::add));
    this.handler = new SpectatorCleanupOnMatchEndEventHandler(lifecycleManager);
  }

  @Test
  @DisplayName("limpia espectadores cuando el match finaliza")
  void cleansUpOnMatchFinished() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    this.handler.handle(new MatchFinishedEvent(this.matchId, p1, p2, PlayerSeat.PLAYER_ONE, 2, 1));

    assertThat(this.repository.findBySpectatorId(this.spectator)
        .flatMap(Spectatorship::getActiveMatchId)).isEmpty();
    assertThat(this.repository.countActiveByMatchId(this.matchId)).isZero();
    assertThat(this.publishedEvents.getFirst()).isInstanceOf(SpectatorCountChanged.class);
  }

  @Test
  @DisplayName("limpia espectadores cuando el match es abandonado")
  void cleansUpOnMatchAbandoned() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    this.handler.handle(
        new MatchAbandonedEvent(this.matchId, p1, p2, PlayerSeat.PLAYER_ONE, PlayerSeat.PLAYER_TWO,
            0, 0));

    assertThat(this.repository.countActiveByMatchId(this.matchId)).isZero();
  }

  @Test
  @DisplayName("limpia espectadores cuando el match es cancelado")
  void cleansUpOnMatchCancelled() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    this.handler.handle(new MatchCancelledEvent(this.matchId, p1, p2));

    assertThat(this.repository.countActiveByMatchId(this.matchId)).isZero();
  }

  @Test
  @DisplayName("limpia espectadores cuando el match es forfeited")
  void cleansUpOnMatchForfeited() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    this.handler.handle(new MatchForfeitedEvent(this.matchId, p1, p2, PlayerSeat.PLAYER_ONE, 2, 0));

    assertThat(this.repository.countActiveByMatchId(this.matchId)).isZero();
  }

  @Test
  @DisplayName("ignora eventos que no son de fin de match")
  void ignoresNonEndEvents() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var inner = new TurnChangedEvent(PlayerSeat.PLAYER_ONE);
    final var envelope = new MatchEventEnvelope(this.matchId, p1, p2, inner);

    this.handler.handle(envelope);

    assertThat(this.repository.countActiveByMatchId(this.matchId)).isEqualTo(1);
    assertThat(this.repository.findBySpectatorId(this.spectator)
        .flatMap(Spectatorship::getActiveMatchId)).contains(this.matchId);
    assertThat(this.publishedEvents).isEmpty();
  }

}
