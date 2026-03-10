package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import org.junit.jupiter.api.Test;

class TurnRestorationPolicyTest {

  @Test
  void checkpointBeforeTrucoShouldCaptureCurrentTurnWhenNotInTrucoProgress() {

    final var currentTurn = PlayerId.generate();
    final var previousCheckpoint = PlayerId.generate();

    final var checkpoint = TurnRestorationPolicy.checkpointBeforeTrucoCall(RoundStatus.PLAYING,
        currentTurn, previousCheckpoint);

    assertThat(checkpoint).isEqualTo(currentTurn);
  }

  @Test
  void checkpointBeforeTrucoShouldKeepCheckpointWhenAlreadyInTrucoProgress() {

    final var currentTurn = PlayerId.generate();
    final var previousCheckpoint = PlayerId.generate();

    final var checkpoint = TurnRestorationPolicy.checkpointBeforeTrucoCall(
        RoundStatus.TRUCO_IN_PROGRESS, currentTurn, previousCheckpoint);

    assertThat(checkpoint).isEqualTo(previousCheckpoint);
  }

  @Test
  void checkpointBeforeEnvidoShouldUseTrucoCheckpointWhenPresent() {

    final var currentTurn = PlayerId.generate();
    final var trucoCheckpoint = PlayerId.generate();

    final var checkpoint = TurnRestorationPolicy.checkpointBeforeEnvidoCall(
        RoundStatus.TRUCO_IN_PROGRESS, currentTurn, trucoCheckpoint, null);

    assertThat(checkpoint).isEqualTo(trucoCheckpoint);
  }

  @Test
  void checkpointBeforeEnvidoShouldUseCurrentTurnOutsideTrucoProgress() {

    final var currentTurn = PlayerId.generate();

    final var checkpoint = TurnRestorationPolicy.checkpointBeforeEnvidoCall(RoundStatus.PLAYING,
        currentTurn, null, null);

    assertThat(checkpoint).isEqualTo(currentTurn);
  }

  @Test
  void shouldCancelTrucoOnlyForInitialTrucoInProgress() {

    assertThat(TurnRestorationPolicy.shouldCancelTrucoOnEnvido(RoundStatus.TRUCO_IN_PROGRESS,
        TrucoCall.TRUCO)).isTrue();

    assertThat(TurnRestorationPolicy.shouldCancelTrucoOnEnvido(RoundStatus.TRUCO_IN_PROGRESS,
        TrucoCall.RETRUCO)).isFalse();

    assertThat(TurnRestorationPolicy.shouldCancelTrucoOnEnvido(RoundStatus.PLAYING,
        TrucoCall.TRUCO)).isFalse();
  }

}
