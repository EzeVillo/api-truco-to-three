package com.villo.truco.domain.model.rematch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.rematch.events.RematchPlayerWantsRematchEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionClosedByLeaveEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionConfirmedEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionExpiredEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionOpenedEvent;
import com.villo.truco.domain.model.rematch.exceptions.BotCannotLeaveRematchSessionException;
import com.villo.truco.domain.model.rematch.exceptions.NotParticipantOfRematchSessionException;
import com.villo.truco.domain.model.rematch.exceptions.RematchSessionExpiredException;
import com.villo.truco.domain.model.rematch.exceptions.RematchSessionNotOpenException;
import com.villo.truco.domain.model.rematch.valueobjects.RematchPlayerChoice;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionStatus;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RematchSessionTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;
  private MatchId originMatchId;
  private Instant now;
  private Duration ttl;

  @BeforeEach
  void setUp() {

    playerOne = PlayerId.generate();
    playerTwo = PlayerId.generate();
    originMatchId = MatchId.generate();
    now = Instant.now();
    ttl = Duration.ofMinutes(2);
  }

  @Nested
  @DisplayName("open")
  class Open {

    @Test
    @DisplayName("creates session in OPEN status with UNDECIDED choices for both human players")
    void opensWithUndecidedChoices() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);

      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.OPEN);
      assertThat(session.getPlayerOneChoice()).isEqualTo(RematchPlayerChoice.UNDECIDED);
      assertThat(session.getPlayerTwoChoice()).isEqualTo(RematchPlayerChoice.UNDECIDED);
      assertThat(session.getPlayerOneId()).isEqualTo(playerOne);
      assertThat(session.getPlayerTwoId()).isEqualTo(playerTwo);
      assertThat(session.getGamesToWin()).isEqualTo(2);
    }

    @Test
    @DisplayName("sets playerTwo choice to WANTS_REMATCH when playerTwo is bot")
    void botPlayerTwoStartsWithWantsRematch() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, true,
          now, ttl);

      assertThat(session.getPlayerTwoChoice()).isEqualTo(RematchPlayerChoice.WANTS_REMATCH);
      assertThat(session.isPlayerTwoIsBot()).isTrue();
    }

    @Test
    @DisplayName("emits RematchSessionOpenedEvent")
    void emitsOpenedEvent() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);

      final var events = session.getRematchDomainEvents();
      assertThat(events).hasSize(1);
      assertThat(events.getFirst()).isInstanceOf(RematchSessionOpenedEvent.class);
    }

    @Test
    @DisplayName("sets playerOne choice to WANTS_REMATCH when playerOne is bot")
    void botPlayerOneStartsWithWantsRematch() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, true, false,
          now, ttl);

      assertThat(session.getPlayerOneChoice()).isEqualTo(RematchPlayerChoice.WANTS_REMATCH);
      assertThat(session.getPlayerTwoChoice()).isEqualTo(RematchPlayerChoice.UNDECIDED);
      assertThat(session.isPlayerOneIsBot()).isTrue();
    }

  }

  @Nested
  @DisplayName("chooseRematch")
  class ChooseRematch {

    @Test
    @DisplayName("playerOne choosing sets their choice to WANTS_REMATCH and emits event")
    void playerOneChooses() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      session.clearDomainEvents();

      session.chooseRematch(playerOne, now, MatchId.generate());

      assertThat(session.getPlayerOneChoice()).isEqualTo(RematchPlayerChoice.WANTS_REMATCH);
      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.OPEN);
      final var events = session.getRematchDomainEvents();
      assertThat(events).hasSize(1);
      assertThat(events.getFirst()).isInstanceOf(RematchPlayerWantsRematchEvent.class);
    }

    @Test
    @DisplayName("both players choosing confirms the session with inverted seats")
    void mutualChoiceConfirmsWithInvertedSeats() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      session.clearDomainEvents();

      session.chooseRematch(playerOne, now, MatchId.generate());
      session.chooseRematch(playerTwo, now, MatchId.generate());

      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.CONFIRMED);
      final var confirmed = session.getRematchDomainEvents().stream()
          .filter(e -> e instanceof RematchSessionConfirmedEvent)
          .map(e -> (RematchSessionConfirmedEvent) e).findFirst().orElseThrow();
      assertThat(confirmed.getNewPlayerOneId()).isEqualTo(playerTwo);
      assertThat(confirmed.getNewPlayerTwoId()).isEqualTo(playerOne);
    }

    @Test
    @DisplayName("bot as player one: confirmed event swaps seats so human becomes new player one")
    void botAsPlayerOneConfirmedEventHasInvertedSeats() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, true, false,
          now, ttl);
      session.clearDomainEvents();

      session.chooseRematch(playerTwo, now, MatchId.generate());

      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.CONFIRMED);
      final var confirmed = session.getRematchDomainEvents().stream()
          .filter(e -> e instanceof RematchSessionConfirmedEvent)
          .map(e -> (RematchSessionConfirmedEvent) e).findFirst().orElseThrow();
      assertThat(confirmed.getNewPlayerOneId()).isEqualTo(playerTwo);
      assertThat(confirmed.getNewPlayerTwoId()).isEqualTo(playerOne);
    }

    @Test
    @DisplayName("bot match confirms immediately when playerOne chooses")
    void botMatchConfirmsImmediately() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, true,
          now, ttl);
      session.clearDomainEvents();

      session.chooseRematch(playerOne, now, MatchId.generate());

      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.CONFIRMED);
    }

    @Test
    @DisplayName("bot match with bot as player one confirms immediately when human (player two) chooses")
    void botAsPlayerOneConfirmsImmediatelyWhenHumanChooses() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, true, false,
          now, ttl);
      session.clearDomainEvents();

      session.chooseRematch(playerTwo, now, MatchId.generate());

      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.CONFIRMED);
    }

    @Test
    @DisplayName("calling chooseRematch with bot player-one actor is a no-op")
    void botAsPlayerOneChooseIsNoOp() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, true, false,
          now, ttl);
      session.clearDomainEvents();

      session.chooseRematch(playerOne, now, MatchId.generate());

      assertThat(session.getRematchDomainEvents()).isEmpty();
      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.OPEN);
    }

    @Test
    @DisplayName("choosing is idempotent when already WANTS_REMATCH")
    void idempotentWhenAlreadyWantsRematch() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      session.clearDomainEvents();
      session.chooseRematch(playerOne, now, MatchId.generate());
      session.clearDomainEvents();

      session.chooseRematch(playerOne, now, MatchId.generate());

      assertThat(session.getRematchDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("throws when player is not a participant")
    void throwsWhenNotParticipant() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      final var stranger = PlayerId.generate();

      assertThatThrownBy(
          () -> session.chooseRematch(stranger, now, MatchId.generate())).isInstanceOf(
          NotParticipantOfRematchSessionException.class);
    }

    @Test
    @DisplayName("throws when session is expired by time")
    void throwsWhenExpired() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      final var afterExpiry = now.plus(ttl).plusSeconds(1);

      assertThatThrownBy(
          () -> session.chooseRematch(playerOne, afterExpiry, MatchId.generate())).isInstanceOf(
          RematchSessionExpiredException.class);
      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.EXPIRED);
    }

    @Test
    @DisplayName("throws when session is already closed")
    void throwsWhenAlreadyClosed() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      session.leave(playerOne);
      session.clearDomainEvents();

      assertThatThrownBy(
          () -> session.chooseRematch(playerTwo, now, MatchId.generate())).isInstanceOf(
          RematchSessionNotOpenException.class);
    }

  }

  @Nested
  @DisplayName("leave")
  class Leave {

    @Test
    @DisplayName("playerOne leaving closes session and emits event to playerTwo")
    void playerOneLeavesNotifiesPlayerTwo() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      session.clearDomainEvents();

      session.leave(playerOne);

      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.CLOSED_BY_LEAVE);
      assertThat(session.getPlayerOneChoice()).isEqualTo(RematchPlayerChoice.LEFT);
      final var events = session.getRematchDomainEvents();
      assertThat(events).hasSize(1);
      final var closed = (RematchSessionClosedByLeaveEvent) events.getFirst();
      assertThat(closed.getActorId()).isEqualTo(playerOne);
      assertThat(closed.getOtherPlayerId()).isEqualTo(playerTwo);
    }

    @Test
    @DisplayName("throws when bot as player two tries to leave")
    void throwsWhenBotTriesToLeave() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, true,
          now, ttl);

      assertThatThrownBy(() -> session.leave(playerTwo)).isInstanceOf(
          BotCannotLeaveRematchSessionException.class);
    }

    @Test
    @DisplayName("throws BotCannotLeaveRematchSessionException when bot as player one tries to leave")
    void throwsWhenBotAsPlayerOneTriesToLeave() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, true, false,
          now, ttl);

      assertThatThrownBy(() -> session.leave(playerOne)).isInstanceOf(
          BotCannotLeaveRematchSessionException.class);
    }

    @Test
    @DisplayName("throws when stranger tries to leave")
    void throwsWhenStrangerTriesToLeave() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      final var stranger = PlayerId.generate();

      assertThatThrownBy(() -> session.leave(stranger)).isInstanceOf(
          NotParticipantOfRematchSessionException.class);
    }

    @Test
    @DisplayName("throws RematchPlayerAlreadyLeftException when player already chose WANTS_REMATCH then tries to choose again after leaving")
    void throwsWhenAlreadyLeft() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      session.leave(playerOne);
      session.clearDomainEvents();

      assertThatThrownBy(
          () -> session.chooseRematch(playerTwo, now, MatchId.generate())).isInstanceOf(
          RematchSessionNotOpenException.class);
    }

  }

  @Nested
  @DisplayName("expireIfNeeded")
  class ExpireIfNeeded {

    @Test
    @DisplayName("transitions to EXPIRED when now is after expiresAt")
    void expiresWhenPastDeadline() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      session.clearDomainEvents();
      final var afterExpiry = now.plus(ttl).plusSeconds(1);

      session.expireIfNeeded(afterExpiry);

      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.EXPIRED);
      assertThat(session.getRematchDomainEvents()).hasSize(1);
      assertThat(session.getRematchDomainEvents().getFirst()).isInstanceOf(
          RematchSessionExpiredEvent.class);
    }

    @Test
    @DisplayName("does nothing when session is still within TTL")
    void doesNothingWhenNotExpired() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      session.clearDomainEvents();

      session.expireIfNeeded(now.plusSeconds(10));

      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.OPEN);
      assertThat(session.getRematchDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("is a no-op when session is already closed")
    void noOpWhenAlreadyClosed() {

      final var session = RematchSession.open(originMatchId, playerOne, playerTwo, 2, false, false,
          now, ttl);
      session.leave(playerOne);
      session.clearDomainEvents();

      session.expireIfNeeded(now.plus(ttl).plusSeconds(1));

      assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.CLOSED_BY_LEAVE);
      assertThat(session.getRematchDomainEvents()).isEmpty();
    }

  }

}
