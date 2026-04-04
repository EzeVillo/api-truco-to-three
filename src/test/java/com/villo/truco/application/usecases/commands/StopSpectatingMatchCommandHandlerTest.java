package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.StopSpectatingMatchCommand;
import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.SpectatorCountChanged;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.model.spectator.exceptions.NotSpectatingException;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
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

@DisplayName("StopSpectatingMatchCommandHandler")
class StopSpectatingMatchCommandHandlerTest {

  private PlayerId spectator;
  private MatchId matchId;
  private InMemorySpectatorshipRepository repository;
  private List<ApplicationEvent> publishedEvents;
  private StopSpectatingMatchCommandHandler handler;

  @BeforeEach
  void setUp() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    this.spectator = PlayerId.generate();
    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    this.matchId = match.getId();

    this.repository = new InMemorySpectatorshipRepository();
    this.publishedEvents = new ArrayList<>();

    final MatchQueryRepository matchRepo = new MatchQueryRepository() {

      @Override
      public Optional<Match> findById(final MatchId id) {

        return id.equals(matchId) ? Optional.of(match) : Optional.empty();
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
        new SpectatorCountChangedPublisher(matchRepo, this.repository, this.publishedEvents::add));
    this.handler = new StopSpectatingMatchCommandHandler(lifecycleManager);
  }

  @Test
  @DisplayName("desregistra espectador y publica evento de conteo")
  void success() {

    final var spectatorship = Spectatorship.create(this.spectator);
    spectatorship.startWatching(this.matchId);
    this.repository.save(spectatorship);

    this.handler.handle(new StopSpectatingMatchCommand(this.spectator));

    assertThat(this.repository.findBySpectatorId(this.spectator)
        .flatMap(Spectatorship::getActiveMatchId)).isEmpty();
    assertThat(this.repository.countActiveByMatchId(this.matchId)).isZero();
    assertThat(this.publishedEvents).hasSize(1);
    assertThat(this.publishedEvents.getFirst()).isInstanceOf(SpectatorCountChanged.class);

    final var event = (SpectatorCountChanged) this.publishedEvents.getFirst();
    assertThat(event.count()).isZero();
  }

  @Test
  @DisplayName("falla si el espectador no esta especteando")
  void failsWhenNotSpectating() {

    assertThatThrownBy(
        () -> this.handler.handle(new StopSpectatingMatchCommand(this.spectator))).isInstanceOf(
        NotSpectatingException.class);
  }

}
