package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatchReadyPolicyTest {

  @Test
  void shouldMarkPlayerOneReadyWhenNotReady() {

    final var state = MatchReadyPolicy.markReady(false, false, true);

    assertThat(state.readyPlayerOne()).isTrue();
    assertThat(state.readyPlayerTwo()).isFalse();
    assertThat(state.changed()).isTrue();
    assertThat(state.bothReady()).isFalse();
  }

  @Test
  void shouldBeIdempotentWhenPlayerOneAlreadyReady() {

    final var state = MatchReadyPolicy.markReady(true, false, true);

    assertThat(state.readyPlayerOne()).isTrue();
    assertThat(state.readyPlayerTwo()).isFalse();
    assertThat(state.changed()).isFalse();
    assertThat(state.bothReady()).isFalse();
  }

  @Test
  void shouldMarkPlayerTwoReadyWhenNotReady() {

    final var state = MatchReadyPolicy.markReady(true, false, false);

    assertThat(state.readyPlayerOne()).isTrue();
    assertThat(state.readyPlayerTwo()).isTrue();
    assertThat(state.changed()).isTrue();
    assertThat(state.bothReady()).isTrue();
  }

}
