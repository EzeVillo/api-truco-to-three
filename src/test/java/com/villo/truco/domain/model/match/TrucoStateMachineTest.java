package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.match.exceptions.InvalidTrucoCallException;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TrucoStateMachineTest {

  private PlayerId playerA;
  private PlayerId playerB;
  private TrucoStateMachine flow;

  @BeforeEach
  void setUp() {

    this.playerA = PlayerId.generate();
    this.playerB = PlayerId.generate();
    this.flow = new TrucoStateMachine();
  }

  @Nested
  @DisplayName("Initial state")
  class InitialState {

    @Test
    void shouldNotHaveBeenCalled() {

      assertThat(flow.hasBeenCalled()).isFalse();
    }

    @Test
    void shouldHaveOnePointAtStake() {

      assertThat(flow.getPointsAtStake()).isEqualTo(1);
    }

    @Test
    void shouldHaveNullCurrentCall() {

      assertThat(flow.getCurrentCall()).isNull();
    }

    @Test
    void shouldHaveNullCaller() {

      assertThat(flow.getCaller()).isNull();
    }

    @Test
    void anyPlayerCanEscalate() {

      assertThat(flow.canEscalate(playerA)).isTrue();
      assertThat(flow.canEscalate(playerB)).isTrue();
    }

  }

  @Nested
  @DisplayName("Call transitions")
  class CallTransitions {

    @Test
    void firstCallSetsTruco() {

      final var result = flow.call(playerA);

      assertThat(result).isEqualTo(TrucoCall.TRUCO);
      assertThat(flow.getCurrentCall()).isEqualTo(TrucoCall.TRUCO);
      assertThat(flow.getCaller()).isEqualTo(playerA);
      assertThat(flow.hasBeenCalled()).isTrue();
    }

    @Test
    void opponentCanEscalateToRetruco() {

      flow.call(playerA);
      final var result = flow.call(playerB);

      assertThat(result).isEqualTo(TrucoCall.RETRUCO);
      assertThat(flow.getCaller()).isEqualTo(playerB);
    }

    @Test
    void originalCallerCanEscalateToValeCuatro() {

      flow.call(playerA);
      flow.call(playerB);
      final var result = flow.call(playerA);

      assertThat(result).isEqualTo(TrucoCall.VALE_CUATRO);
      assertThat(flow.getCaller()).isEqualTo(playerA);
    }

    @Test
    void samePlayerCannotCallTwice() {

      flow.call(playerA);

      assertThatThrownBy(() -> flow.call(playerA)).isInstanceOf(InvalidTrucoCallException.class);
    }

    @Test
    void cannotEscalateBeyondValeCuatro() {

      flow.call(playerA);
      flow.call(playerB);
      flow.call(playerA);

      assertThatThrownBy(() -> flow.call(playerB)).isInstanceOf(InvalidTrucoCallException.class);
    }

  }

  @Nested
  @DisplayName("canEscalate")
  class CanEscalate {

    @Test
    void callerCannotEscalateOwnCall() {

      flow.call(playerA);

      assertThat(flow.canEscalate(playerA)).isFalse();
      assertThat(flow.canEscalate(playerB)).isTrue();
    }

    @Test
    void noOneCanEscalateAfterValeCuatro() {

      flow.call(playerA);
      flow.call(playerB);
      flow.call(playerA);

      assertThat(flow.canEscalate(playerA)).isFalse();
      assertThat(flow.canEscalate(playerB)).isFalse();
    }

  }

  @Nested
  @DisplayName("Accept")
  class Accept {

    @Test
    void acceptTrucoSetsTwoPoints() {

      flow.call(playerA);
      flow.accept();

      assertThat(flow.getPointsAtStake()).isEqualTo(2);
    }

    @Test
    void acceptRetrucoSetsThreePoints() {

      flow.call(playerA);
      flow.call(playerB);
      flow.accept();

      assertThat(flow.getPointsAtStake()).isEqualTo(3);
    }

    @Test
    void acceptValeCuatroSetsFourPoints() {

      flow.call(playerA);
      flow.call(playerB);
      flow.call(playerA);
      flow.accept();

      assertThat(flow.getPointsAtStake()).isEqualTo(4);
    }

  }

  @Nested
  @DisplayName("Points calculation")
  class PointsCalculation {

    @Test
    void trucoRejectedGivesOnePoint() {

      flow.call(playerA);

      assertThat(flow.pointsIfRejected()).isEqualTo(1);
    }

    @Test
    void retrucoRejectedGivesTwoPoints() {

      flow.call(playerA);
      flow.call(playerB);

      assertThat(flow.pointsIfRejected()).isEqualTo(2);
    }

    @Test
    void valeCuatroRejectedGivesThreePoints() {

      flow.call(playerA);
      flow.call(playerB);
      flow.call(playerA);

      assertThat(flow.pointsIfRejected()).isEqualTo(3);
    }

    @Test
    void trucoAcceptedGivesTwoPoints() {

      flow.call(playerA);

      assertThat(flow.pointsIfAccepted()).isEqualTo(2);
    }

    @Test
    void valeCuatroAcceptedGivesFourPoints() {

      flow.call(playerA);
      flow.call(playerB);
      flow.call(playerA);

      assertThat(flow.pointsIfAccepted()).isEqualTo(4);
    }

  }

  @Nested
  @DisplayName("Cancel")
  class Cancel {

    @Test
    void cancelResetsCallState() {

      flow.call(playerA);
      flow.cancel();

      assertThat(flow.hasBeenCalled()).isFalse();
      assertThat(flow.getCurrentCall()).isNull();
      assertThat(flow.getCaller()).isNull();
    }

    @Test
    void cancelPreservesDefaultPointsAtStake() {

      flow.call(playerA);
      flow.cancel();

      assertThat(flow.getPointsAtStake()).isEqualTo(1);
    }

    @Test
    void canCallAgainAfterCancel() {

      flow.call(playerA);
      flow.cancel();

      final var result = flow.call(playerB);

      assertThat(result).isEqualTo(TrucoCall.TRUCO);
    }

  }

}
