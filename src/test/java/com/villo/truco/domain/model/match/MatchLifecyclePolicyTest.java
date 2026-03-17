package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.Test;

class MatchLifecyclePolicyTest {

  @Test
  void shouldStartFirstGameWithPlayerOneAsMano() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    final var state = MatchLifecyclePolicy.startNextGame(0, playerOne, playerTwo);

    assertThat(state.gameNumber()).isEqualTo(1);
    assertThat(state.scorePlayerOne()).isZero();
    assertThat(state.scorePlayerTwo()).isZero();
    assertThat(state.roundNumber()).isZero();
    assertThat(state.firstManoOfGame()).isEqualTo(playerOne);
  }

  @Test
  void shouldStartSecondGameWithPlayerTwoAsMano() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    final var state = MatchLifecyclePolicy.startNextGame(1, playerOne, playerTwo);

    assertThat(state.gameNumber()).isEqualTo(2);
    assertThat(state.firstManoOfGame()).isEqualTo(playerTwo);
  }

  @Test
  void shouldResolveRoundManoAlternatingFromFirstMano() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    assertThat(MatchLifecyclePolicy.resolveRoundMano(1, playerOne, playerOne, playerTwo)).isEqualTo(
        playerOne);
    assertThat(MatchLifecyclePolicy.resolveRoundMano(2, playerOne, playerOne, playerTwo)).isEqualTo(
        playerTwo);
    assertThat(MatchLifecyclePolicy.resolveRoundMano(3, playerOne, playerOne, playerTwo)).isEqualTo(
        playerOne);
  }

}
