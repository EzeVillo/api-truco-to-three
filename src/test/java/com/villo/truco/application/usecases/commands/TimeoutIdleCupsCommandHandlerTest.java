package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TimeoutIdleCupsCommandHandler")
class TimeoutIdleCupsCommandHandlerTest {

  private Cup waitingForPlayersCup() {

    return Cup.create(PlayerId.generate(), 4, GamesToPlay.of(3));
  }

  private Cup waitingForStartCup() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3));
    cup.join(p2, cup.getInviteCode());
    cup.join(p3, cup.getInviteCode());
    cup.join(p4, cup.getInviteCode());
    return cup;
  }

  private TimeoutIdleCupsCommandHandler handlerFor(final Map<CupId, Cup> cups,
      final AtomicReference<Cup> savedCup) {

    final CupQueryRepository queryRepo = new CupQueryRepository() {

      @Override
      public Optional<Cup> findById(final CupId cupId) {

        return Optional.ofNullable(cups.get(cupId));
      }

      @Override
      public Optional<Cup> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(
          final com.villo.truco.domain.model.match.valueobjects.MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findInProgressByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findWaitingByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<CupId> findIdleCupIds(final Instant idleSince) {

        return List.copyOf(cups.keySet());
      }
    };

    final CupRepository cupRepository = savedCup::set;
    final TransactionalRunner transactionalRunner = Runnable::run;

    return new TimeoutIdleCupsCommandHandler(queryRepo, cupRepository, transactionalRunner,
        Duration.ofMinutes(10), (id, participants, events) -> {
    });
  }

  @Test
  @DisplayName("copa en WAITING_FOR_PLAYERS se cancela")
  void waitingForPlayersCupIsCancelled() {

    final var cup = waitingForPlayersCup();
    final var savedCup = new AtomicReference<Cup>();
    final var handler = handlerFor(Map.of(cup.getId(), cup), savedCup);

    handler.handle();

    assertThat(savedCup.get()).isNotNull();
    assertThat(savedCup.get().getStatus()).isEqualTo(CupStatus.CANCELLED);
  }

  @Test
  @DisplayName("copa en WAITING_FOR_START se cancela")
  void waitingForStartCupIsCancelled() {

    final var cup = waitingForStartCup();
    final var savedCup = new AtomicReference<Cup>();
    final var handler = handlerFor(Map.of(cup.getId(), cup), savedCup);

    handler.handle();

    assertThat(savedCup.get()).isNotNull();
    assertThat(savedCup.get().getStatus()).isEqualTo(CupStatus.CANCELLED);
  }

  @Test
  @DisplayName("copa ya CANCELLED es ignorada")
  void cancelledCupIsSkipped() {

    final var cup = waitingForPlayersCup();
    cup.cancel();
    final var savedCup = new AtomicReference<Cup>();
    final var handler = handlerFor(Map.of(cup.getId(), cup), savedCup);

    handler.handle();

    assertThat(savedCup.get()).isNull();
  }

  @Test
  @DisplayName("lista vacía de idle cups no hace nada")
  void emptyIdleListDoesNothing() {

    final var savedCup = new AtomicReference<Cup>();
    final var handler = handlerFor(Map.of(), savedCup);

    handler.handle();

    assertThat(savedCup.get()).isNull();
  }

  @Test
  @DisplayName("excepción en una copa no afecta las demás")
  void exceptionInOneCupDoesNotAffectOthers() {

    final var goodCup = waitingForPlayersCup();
    final var savedCup = new AtomicReference<Cup>();

    final CupQueryRepository queryRepo = new CupQueryRepository() {

      @Override
      public Optional<Cup> findById(final CupId cupId) {

        if (cupId.equals(goodCup.getId())) {
          return Optional.of(goodCup);
        }
        throw new RuntimeException("simulated failure");
      }

      @Override
      public Optional<Cup> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(
          final com.villo.truco.domain.model.match.valueobjects.MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findInProgressByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findWaitingByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<CupId> findIdleCupIds(final Instant idleSince) {

        return List.of(CupId.generate(), goodCup.getId());
      }
    };

    final var handler = new TimeoutIdleCupsCommandHandler(queryRepo, savedCup::set, Runnable::run,
        Duration.ofMinutes(10), (id, participants, events) -> {
    });

    handler.handle();

    assertThat(savedCup.get()).isNotNull();
    assertThat(savedCup.get().getStatus()).isEqualTo(CupStatus.CANCELLED);
  }

}
