package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.Test;

class TrucoEscalationSpecificationTest {

  @Test
  void allowsFirstCall() {

    final var player = PlayerId.generate();

    final var allowed = TrucoEscalationSpecification.isSatisfiedBy(null, null, player);

    assertThat(allowed).isTrue();
    assertThat(TrucoEscalationSpecification.nextCall(null)).isEqualTo(TrucoCall.TRUCO);
  }

  @Test
  void blocksSameCaller() {

    final var player = PlayerId.generate();

    final var allowed = TrucoEscalationSpecification.isSatisfiedBy(TrucoCall.TRUCO, player, player);

    assertThat(allowed).isFalse();
  }

  @Test
  void blocksEscalationAfterValeCuatro() {

    final var caller = PlayerId.generate();
    final var requester = PlayerId.generate();

    final var allowed = TrucoEscalationSpecification.isSatisfiedBy(TrucoCall.VALE_CUATRO, caller,
        requester);

    assertThat(allowed).isFalse();
  }

}