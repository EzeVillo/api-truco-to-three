package com.villo.truco.domain.model.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TrucoDecisionPolicyTest {

  private static final int POINTS_TO_WIN = 3;
  private static final int POINTS_TO_WIN_FIVE = 5;

  private static final BotTrucoCall TRUCO = new BotTrucoCall(2, 1);
  private static final BotTrucoCall RETRUCO = new BotTrucoCall(3, 2);

  private static final BotPersonality AGGRESSIVE = new BotPersonality(100, 1, 100, 50, 50);
  private static final BotPersonality PASSIVE = new BotPersonality(1, 1, 1, 1, 1);

  private static final Random ALWAYS_ZERO = new Random() {
    @Override
    public double nextDouble() {

      return 0.0;
    }
  };
  private static final Random ALWAYS_ONE = new Random() {
    @Override
    public double nextDouble() {

      return 1.0;
    }
  };

  @Test
  void decideCall_safety_neverCallsIfRejectionKillsBot() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    final var result = policy.decideCall(RETRUCO, 1.0, 2, 0, POINTS_TO_WIN);
    assertThat(result).isEmpty();
  }

  @Test
  void decideCall_scoreTrap_alwaysCallsWhenRivalDiesIfAccepted() {

    final var policy = new TrucoDecisionPolicy(PASSIVE, ALWAYS_ONE);
    final var result = policy.decideCall(TRUCO, 0.0, 0, 2, POINTS_TO_WIN);
    assertThat(result).contains(TRUCO);
  }

  @Test
  void decideCall_rejectionWins_andBothWouldExceed_weakHand_callsAsTrap() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    final var result = policy.decideCall(TRUCO, 0.1, 2, 2, POINTS_TO_WIN);
    assertThat(result).contains(TRUCO);
  }

  @Test
  void decideCall_rejectionWins_andBothWouldExceed_strongHand_doesNotCall() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    final var result = policy.decideCall(TRUCO, 0.9, 2, 2, POINTS_TO_WIN);
    assertThat(result).isEmpty();
  }

  @Test
  void decideCall_rejectionWins_andAcceptedOnlyKillsBot_doesNotCall() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    final var result = policy.decideCall(TRUCO, 0.1, 2, 0, POINTS_TO_WIN);
    assertThat(result).isEmpty();
  }

  @Test
  void decideCall_usesDynamicPointsToWin() {

    final var policy = new TrucoDecisionPolicy(PASSIVE, ALWAYS_ONE);
    final var result = policy.decideCall(TRUCO, 0.1, 2, 0, POINTS_TO_WIN_FIVE);
    assertThat(result).isEmpty();
  }

  @Test
  void decideCall_withoutAvailableCall_returnsEmpty() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    assertThat(policy.decideCall(null, 1.0, 0, 0, POINTS_TO_WIN)).isEmpty();
  }

  @Test
  void decideRaise_safety_neverRaisesIfRejectionKillsBot() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    assertThat(policy.decideRaise(RETRUCO, 1.0, 2, 0, POINTS_TO_WIN)).isEmpty();
  }

  @Test
  void decideRaise_pureUpside_alwaysRaisesWhenOnlyRivalDies() {

    final var policy = new TrucoDecisionPolicy(PASSIVE, ALWAYS_ONE);
    assertThat(policy.decideRaise(RETRUCO, 0.0, 0, 2, POINTS_TO_WIN)).contains(RETRUCO);
  }

  @Test
  void decideRaise_bothDie_weakHand_raisesAsWeakerWillLoseRound() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    assertThat(policy.decideRaise(TRUCO, 0.1, 2, 2, POINTS_TO_WIN)).contains(TRUCO);
  }

  @Test
  void decideRaise_bothDie_strongHand_doesNotRaise() {

    final var policy = new TrucoDecisionPolicy(PASSIVE, ALWAYS_ONE);
    assertThat(policy.decideRaise(TRUCO, 0.95, 2, 2, POINTS_TO_WIN)).isEmpty();
  }

  @Test
  void decideRaise_rejectionWins_andAcceptedOnlyKillsBot_doesNotRaise() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    assertThat(policy.decideRaise(RETRUCO, 0.1, 1, 0, POINTS_TO_WIN)).isEmpty();
  }

  @Test
  void decideRaise_withoutAvailableRaise_returnsEmpty() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    assertThat(policy.decideRaise(null, 1.0, 0, 0, POINTS_TO_WIN)).isEmpty();
  }

  @Test
  void decideResponse_botSafetyExceeds_returnsReject() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    final var result = policy.decideResponse(TRUCO, 0.0, 2, 0, POINTS_TO_WIN);
    assertThat(result).isEqualTo(BotTrucoResponse.NO_QUIERO);
  }

  @Test
  void decideResponse_rejectionGivesRivalExactWin_acceptsWhenSafe() {

    final var policy = new TrucoDecisionPolicy(PASSIVE, ALWAYS_ONE);
    final var result = policy.decideResponse(TRUCO, 0.0, 0, 2, POINTS_TO_WIN);
    assertThat(result).isEqualTo(BotTrucoResponse.QUIERO);
  }

  @Test
  void decideResponse_rejectionGivesRivalExactWin_butAcceptKillsBot_returnsReject() {

    final var policy = new TrucoDecisionPolicy(PASSIVE, ALWAYS_ONE);
    final var result = policy.decideResponse(TRUCO, 0.0, 2, 2, POINTS_TO_WIN);
    assertThat(result).isEqualTo(BotTrucoResponse.NO_QUIERO);
  }

  @Test
  void decideResponse_probabilisticAccept_returnsAccept() {

    final var policy = new TrucoDecisionPolicy(AGGRESSIVE, ALWAYS_ZERO);
    final var result = policy.decideResponse(TRUCO, 1.0, 0, 0, POINTS_TO_WIN);
    assertThat(result).isEqualTo(BotTrucoResponse.QUIERO);
  }

  @Test
  void decideResponse_probabilisticReject_returnsReject() {

    final var policy = new TrucoDecisionPolicy(PASSIVE, ALWAYS_ONE);
    final var result = policy.decideResponse(TRUCO, 0.0, 0, 0, POINTS_TO_WIN);
    assertThat(result).isEqualTo(BotTrucoResponse.NO_QUIERO);
  }

}
