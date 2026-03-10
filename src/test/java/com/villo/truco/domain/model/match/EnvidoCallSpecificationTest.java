package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnvidoCallSpecificationTest {

  @Test
  @DisplayName("accepts envido when first hand and no conflicting flow")
  void acceptsValidEnvidoCall() {

    final var decision = EnvidoCallSpecification.evaluate(RoundStatus.PLAYING, true, false, false,
        false, null);

    assertThat(decision.satisfied()).isTrue();
    assertThat(decision.reason()).isNull();
  }

  @Test
  @DisplayName("rejects envido when not first hand")
  void rejectsWhenNotFirstHand() {

    final var decision = EnvidoCallSpecification.evaluate(RoundStatus.PLAYING, false, false, false,
        false, null);

    assertThat(decision.satisfied()).isFalse();
    assertThat(decision.reason()).isEqualTo("El envido solo se puede cantar en la primera mano");
  }

  @Test
  @DisplayName("rejects envido when truco has already been accepted")
  void rejectsAfterAcceptedTruco() {

    final var decision = EnvidoCallSpecification.evaluate(RoundStatus.PLAYING, true, false, false,
        true, TrucoCall.TRUCO);

    assertThat(decision.satisfied()).isFalse();
    assertThat(decision.reason()).isEqualTo("No podes cantar envido despues de aceptar el truco");
  }

  @Test
  @DisplayName("rejects envido during retruco flow")
  void rejectsWhenTrucoIsNotTrucoCall() {

    final var decision = EnvidoCallSpecification.evaluate(RoundStatus.TRUCO_IN_PROGRESS, true,
        false, false, true, TrucoCall.RETRUCO);

    assertThat(decision.satisfied()).isFalse();
    assertThat(decision.reason()).isEqualTo("Solo podes cantar envido cuando el truco es TRUCO");
  }

}