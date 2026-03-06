package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.match.exceptions.EnvidoNotAllowedException;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnvidoFlowTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;
  private EnvidoFlow flow;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
    this.flow = new EnvidoFlow();
  }

  @Nested
  @DisplayName("Initial state")
  class InitialState {

    @Test
    void shouldNotBeResolved() {

      assertThat(flow.isResolved()).isFalse();
    }

    @Test
    void shouldBeEmpty() {

      assertThat(flow.isEmpty()).isTrue();
    }

    @Test
    void chainShouldBeEmpty() {

      assertThat(flow.getChain()).isEmpty();
    }

    @Test
    void canRaiseWithAnything() {

      assertThat(flow.canRaiseWith(EnvidoCall.ENVIDO)).isTrue();
      assertThat(flow.canRaiseWith(EnvidoCall.REAL_ENVIDO)).isTrue();
      assertThat(flow.canRaiseWith(EnvidoCall.FALTA_ENVIDO)).isTrue();
    }

  }

  @Nested
  @DisplayName("Call transitions")
  class CallTransitions {

    @Test
    void firstCallAddsToChain() {

      flow.call(EnvidoCall.ENVIDO);

      assertThat(flow.getChain()).containsExactly(EnvidoCall.ENVIDO);
      assertThat(flow.isEmpty()).isFalse();
    }

    @Test
    void canCallEnvidoTwice() {

      flow.call(EnvidoCall.ENVIDO);

      assertThatNoException().isThrownBy(() -> flow.call(EnvidoCall.ENVIDO));
      assertThat(flow.getChain()).containsExactly(EnvidoCall.ENVIDO, EnvidoCall.ENVIDO);
    }

    @Test
    @DisplayName("no se puede cantar un tercer ENVIDO")
    void cannotCallThirdEnvido() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.ENVIDO);

      assertThatThrownBy(() -> flow.call(EnvidoCall.ENVIDO)).isInstanceOf(
          EnvidoNotAllowedException.class).hasMessageContaining("más de dos envidos");
    }

    @Test
    void canEscalateEnvidoToRealEnvido() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.REAL_ENVIDO);

      assertThat(flow.getChain()).containsExactly(EnvidoCall.ENVIDO, EnvidoCall.REAL_ENVIDO);
    }

    @Test
    void canEscalateEnvidoToFaltaEnvido() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.FALTA_ENVIDO);

      assertThat(flow.getChain()).containsExactly(EnvidoCall.ENVIDO, EnvidoCall.FALTA_ENVIDO);
    }

    @Test
    void canEscalateRealEnvidoToFaltaEnvido() {

      flow.call(EnvidoCall.REAL_ENVIDO);
      flow.call(EnvidoCall.FALTA_ENVIDO);

      assertThat(flow.getChain()).containsExactly(EnvidoCall.REAL_ENVIDO, EnvidoCall.FALTA_ENVIDO);
    }

    @Test
    void cannotRaiseAfterFaltaEnvido() {

      flow.call(EnvidoCall.FALTA_ENVIDO);

      assertThatThrownBy(() -> flow.call(EnvidoCall.ENVIDO)).isInstanceOf(
          EnvidoNotAllowedException.class);
      assertThatThrownBy(() -> flow.call(EnvidoCall.REAL_ENVIDO)).isInstanceOf(
          EnvidoNotAllowedException.class);
      assertThatThrownBy(() -> flow.call(EnvidoCall.FALTA_ENVIDO)).isInstanceOf(
          EnvidoNotAllowedException.class);
    }

    @Test
    void cannotLowerHierarchyAfterRealEnvido() {

      flow.call(EnvidoCall.REAL_ENVIDO);

      assertThatThrownBy(() -> flow.call(EnvidoCall.ENVIDO)).isInstanceOf(
          EnvidoNotAllowedException.class);
    }

    @Test
    void fullChainEnvidoEnvidoRealEnvidoFaltaEnvido() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.REAL_ENVIDO);
      flow.call(EnvidoCall.FALTA_ENVIDO);

      assertThat(flow.getChain()).containsExactly(EnvidoCall.ENVIDO, EnvidoCall.ENVIDO,
          EnvidoCall.REAL_ENVIDO, EnvidoCall.FALTA_ENVIDO);
    }

  }

  @Nested
  @DisplayName("canRaiseWith")
  class CanRaiseWith {

    @Test
    void afterEnvidoCanRaiseWithAll() {

      flow.call(EnvidoCall.ENVIDO);

      assertThat(flow.canRaiseWith(EnvidoCall.ENVIDO)).isTrue();
      assertThat(flow.canRaiseWith(EnvidoCall.REAL_ENVIDO)).isTrue();
      assertThat(flow.canRaiseWith(EnvidoCall.FALTA_ENVIDO)).isTrue();
    }

    @Test
    void afterTwoEnvidosCannotRaiseWithEnvido() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.ENVIDO);

      assertThat(flow.canRaiseWith(EnvidoCall.ENVIDO)).isFalse();
      assertThat(flow.canRaiseWith(EnvidoCall.REAL_ENVIDO)).isTrue();
      assertThat(flow.canRaiseWith(EnvidoCall.FALTA_ENVIDO)).isTrue();
    }

    @Test
    void afterRealEnvidoOnlyFaltaAllowed() {

      flow.call(EnvidoCall.REAL_ENVIDO);

      assertThat(flow.canRaiseWith(EnvidoCall.ENVIDO)).isFalse();
      assertThat(flow.canRaiseWith(EnvidoCall.REAL_ENVIDO)).isFalse();
      assertThat(flow.canRaiseWith(EnvidoCall.FALTA_ENVIDO)).isTrue();
    }

    @Test
    void afterFaltaEnvidoNothingAllowed() {

      flow.call(EnvidoCall.FALTA_ENVIDO);

      assertThat(flow.canRaiseWith(EnvidoCall.ENVIDO)).isFalse();
      assertThat(flow.canRaiseWith(EnvidoCall.REAL_ENVIDO)).isFalse();
      assertThat(flow.canRaiseWith(EnvidoCall.FALTA_ENVIDO)).isFalse();
    }

  }

  @Nested
  @DisplayName("Resolve")
  class Resolve {

    @Test
    void resolveSetsResolvedFlag() {

      flow.call(EnvidoCall.ENVIDO);
      flow.resolve();

      assertThat(flow.isResolved()).isTrue();
    }

  }

  @Nested
  @DisplayName("Rejected points calculation")
  class RejectedPoints {

    @Test
    void singleCallGivesOnePoint() {

      flow.call(EnvidoCall.ENVIDO);

      assertThat(flow.calculateRejectedPoints()).isEqualTo(1);
    }

    @Test
    void singleRealEnvidoGivesOnePoint() {

      flow.call(EnvidoCall.REAL_ENVIDO);

      assertThat(flow.calculateRejectedPoints()).isEqualTo(1);
    }

    @Test
    void singleFaltaEnvidoGivesOnePoint() {

      flow.call(EnvidoCall.FALTA_ENVIDO);

      assertThat(flow.calculateRejectedPoints()).isEqualTo(1);
    }

    @Test
    void envidoThenRealEnvidoGivesTwoPoints() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.REAL_ENVIDO);

      assertThat(flow.calculateRejectedPoints()).isEqualTo(2);
    }

    @Test
    void envidoEnvidoThenRealEnvidoGivesFourPoints() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.REAL_ENVIDO);

      assertThat(flow.calculateRejectedPoints()).isEqualTo(4);
    }

    @Test
    void envidoThenFaltaEnvidoGivesTwoPoints() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.FALTA_ENVIDO);

      assertThat(flow.calculateRejectedPoints()).isEqualTo(2);
    }

    @Test
    void envidoEnvidoRealEnvidoThenFaltaEnvidoGivesSevenPoints() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.REAL_ENVIDO);
      flow.call(EnvidoCall.FALTA_ENVIDO);

      assertThat(flow.calculateRejectedPoints()).isEqualTo(7);
    }

  }

  @Nested
  @DisplayName("Accepted points calculation")
  class AcceptedPoints {

    @Test
    void singleEnvidoGivesTwoPoints() {

      flow.call(EnvidoCall.ENVIDO);

      assertThat(flow.calculateAcceptedPoints(0, 0, playerOne, playerOne, 15)).isEqualTo(2);
    }

    @Test
    void envidoEnvidoGivesFourPoints() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.ENVIDO);

      assertThat(flow.calculateAcceptedPoints(0, 0, playerOne, playerOne, 15)).isEqualTo(4);
    }

    @Test
    void realEnvidoGivesThreePoints() {

      flow.call(EnvidoCall.REAL_ENVIDO);

      assertThat(flow.calculateAcceptedPoints(0, 0, playerOne, playerOne, 15)).isEqualTo(3);
    }

    @Test
    void envidoRealEnvidoGivesFivePoints() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.REAL_ENVIDO);

      assertThat(flow.calculateAcceptedPoints(0, 0, playerOne, playerOne, 15)).isEqualTo(5);
    }

    @Test
    @DisplayName("FALTA_ENVIDO con rival en 10 puntos y 15 para ganar → 5 puntos")
    void faltaEnvidoCalculatesRemainingPointsForRival() {

      flow.call(EnvidoCall.FALTA_ENVIDO);

      // playerOne wins, rival (playerTwo) has 10, points to win = 15
      assertThat(flow.calculateAcceptedPoints(5, 10, playerOne, playerOne, 15)).isEqualTo(5);
    }

    @Test
    @DisplayName("FALTA_ENVIDO cuando playerTwo gana → usa score de playerOne como rival")
    void faltaEnvidoWhenPlayerTwoWins() {

      flow.call(EnvidoCall.FALTA_ENVIDO);

      // playerTwo wins, rival (playerOne) has 8, points to win = 15
      assertThat(flow.calculateAcceptedPoints(8, 3, playerTwo, playerOne, 15)).isEqualTo(7);
    }

    @Test
    void envidoEnvidoRealEnvidoFaltaEnvidoUsesOnlyFaltaLogic() {

      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.ENVIDO);
      flow.call(EnvidoCall.REAL_ENVIDO);
      flow.call(EnvidoCall.FALTA_ENVIDO);

      // playerOne wins, rival has 12, needs 15 → 3 points
      assertThat(flow.calculateAcceptedPoints(5, 12, playerOne, playerOne, 15)).isEqualTo(3);
    }

  }

  @Nested
  @DisplayName("Chain immutability")
  class ChainImmutability {

    @Test
    void getChainReturnsUnmodifiableList() {

      flow.call(EnvidoCall.ENVIDO);

      assertThatThrownBy(() -> flow.getChain().add(EnvidoCall.REAL_ENVIDO)).isInstanceOf(
          UnsupportedOperationException.class);
    }

  }

}
