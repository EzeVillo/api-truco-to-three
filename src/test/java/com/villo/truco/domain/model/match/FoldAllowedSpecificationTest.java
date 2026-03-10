package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FoldAllowedSpecificationTest {

  @Test
  @DisplayName("rejects fold for mano on first hand without envido resolved and without truco")
  void rejectsBlockedCombination() {

    final var allowed = FoldAllowedSpecification.isSatisfiedBy(true, true, false, false);

    assertThat(allowed).isFalse();
  }

  @Test
  @DisplayName("allows fold for mano on first hand after accepted truco")
  void allowsWhenTrucoWasCalled() {

    final var allowed = FoldAllowedSpecification.isSatisfiedBy(true, true, false, true);

    assertThat(allowed).isTrue();
  }

  @Test
  @DisplayName("allows fold when player is not mano")
  void allowsWhenPlayerIsNotMano() {

    final var allowed = FoldAllowedSpecification.isSatisfiedBy(false, true, false, false);

    assertThat(allowed).isTrue();
  }

}