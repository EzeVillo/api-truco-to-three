package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.Test;

class PlayerInMatchSpecificationTest {

  @Test
  void acceptsEitherPlayerOfMatch() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    assertThat(PlayerInMatchSpecification.isSatisfiedBy(playerOne, playerOne, playerTwo)).isTrue();
    assertThat(PlayerInMatchSpecification.isSatisfiedBy(playerTwo, playerOne, playerTwo)).isTrue();
  }

  @Test
  void rejectsStrangerPlayer() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var stranger = PlayerId.generate();

    assertThat(PlayerInMatchSpecification.isSatisfiedBy(stranger, playerOne, playerTwo)).isFalse();
  }

}