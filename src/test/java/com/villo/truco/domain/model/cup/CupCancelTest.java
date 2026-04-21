package com.villo.truco.domain.model.cup;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cup.cancel()")
class CupCancelTest {

  private Cup waitingForPlayersCup() {

    return Cup.create(PlayerId.generate(), 4, GamesToPlay.of(3), Visibility.PRIVATE);
  }

  private Cup waitingForStartCup() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3), Visibility.PRIVATE);
    cup.join(p2);
    cup.join(p3);
    cup.join(p4);
    return cup;
  }

  private Cup inProgressCup() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3), Visibility.PRIVATE);
    cup.join(p2);
    cup.join(p3);
    cup.join(p4);
    cup.start(p1);
    return cup;
  }

  @Test
  @DisplayName("WAITING_FOR_PLAYERS → cancel → CANCELLED")
  void waitingForPlayersIsCancelled() {

    final var cup = waitingForPlayersCup();

    cup.cancel();

    assertThat(cup.getStatus()).isEqualTo(CupStatus.CANCELLED);
  }

  @Test
  @DisplayName("WAITING_FOR_START → cancel → CANCELLED")
  void waitingForStartIsCancelled() {

    final var cup = waitingForStartCup();

    cup.cancel();

    assertThat(cup.getStatus()).isEqualTo(CupStatus.CANCELLED);
  }

  @Test
  @DisplayName("IN_PROGRESS → cancel → no-op")
  void inProgressIsIgnored() {

    final var cup = inProgressCup();

    cup.cancel();

    assertThat(cup.getStatus()).isEqualTo(CupStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("CANCELLED → cancel → no-op")
  void alreadyCancelledIsIgnored() {

    final var cup = waitingForPlayersCup();
    cup.cancel();

    cup.cancel();

    assertThat(cup.getStatus()).isEqualTo(CupStatus.CANCELLED);
  }

}
