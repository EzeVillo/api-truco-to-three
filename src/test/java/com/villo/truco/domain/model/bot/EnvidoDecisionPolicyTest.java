package com.villo.truco.domain.model.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.bot.exceptions.PendingEnvidoCallRequiredException;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.PendingEnvidoOutcome;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EnvidoDecisionPolicyTest {

  private static final int POINTS_TO_WIN = 3;
  private static final int POINTS_TO_WIN_FIVE = 5;

  private static final BotPersonality NEUTRAL = new BotPersonality(50, 1, 50, 50, 50);

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

  private static BotEnvidoCall call(final int acceptedPointsIfBotWins,
      final int acceptedPointsIfRivalWins, final int rejectedPointsIfRivalDeclines,
      final BotEnvidoLevel level) {

    return new BotEnvidoCall(acceptedPointsIfBotWins, acceptedPointsIfRivalWins,
        rejectedPointsIfRivalDeclines, level);
  }

  private static BotEnvidoCall envido() {

    return call(2, 2, 1, BotEnvidoLevel.ENVIDO);
  }

  private static BotEnvidoCall realEnvido() {

    return call(3, 3, 1, BotEnvidoLevel.REAL_ENVIDO);
  }

  private static BotEnvidoCall faltaEnvido(final int pointsToWin, final int myScore,
      final int rivalScore) {

    return call(pointsToWin - rivalScore, pointsToWin - myScore, 1, BotEnvidoLevel.FALTA_ENVIDO);
  }

  @Test
  void decideResponse_p1_rejectionKillsRival_noQuiero() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ZERO);
    // chain=[envido,envido]: ptsIfBotWins=4, ptsIfRivalWins=4, rejectedPts=2
    final var result = policy.decideResponse(30, 0, 2, POINTS_TO_WIN,
        new PendingEnvidoOutcome(4, 4, 2));
    assertThat(result).isEqualTo(BotEnvidoResponse.NO_QUIERO);
  }

  @Test
  void decideResponse_p2_winWin_quiero() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ONE);
    // chain=[envido]: ptsIfBotWins=2, ptsIfRivalWins=2, rejectedPts=1
    final var result = policy.decideResponse(10, 1, 2, POINTS_TO_WIN,
        new PendingEnvidoOutcome(2, 2, 1));
    assertThat(result).isEqualTo(BotEnvidoResponse.QUIERO);
  }

  @Test
  void decideResponse_p3_bothDie_rejectionGivesRivalWin_quiero() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ONE);
    // chain=[envido]: ptsIfBotWins=2, ptsIfRivalWins=2, rejectedPts=1
    final var result = policy.decideResponse(33, 2, 2, POINTS_TO_WIN,
        new PendingEnvidoOutcome(2, 2, 1));
    assertThat(result).isEqualTo(BotEnvidoResponse.QUIERO);
  }

  @Test
  void decideResponse_p3_bothDie_rejectionNeutral_lowEnvido_quiero() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ONE);
    // chain=[realEnvido]: ptsIfBotWins=3, ptsIfRivalWins=3, rejectedPts=1
    final var result = policy.decideResponse(15, 1, 1, POINTS_TO_WIN,
        new PendingEnvidoOutcome(3, 3, 1));
    assertThat(result).isEqualTo(BotEnvidoResponse.QUIERO);
  }

  @Test
  void decideResponse_p3_bothDie_rejectionNeutral_highEnvido_noQuiero() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ONE);
    // chain=[realEnvido]: ptsIfBotWins=3, ptsIfRivalWins=3, rejectedPts=1
    final var result = policy.decideResponse(30, 1, 1, POINTS_TO_WIN,
        new PendingEnvidoOutcome(3, 3, 1));
    assertThat(result).isEqualTo(BotEnvidoResponse.NO_QUIERO);
  }

  @Test
  void decideResponse_p4_onlyBotDies_noQuiero() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ZERO);
    // chain=[envido]: ptsIfBotWins=2, ptsIfRivalWins=2, rejectedPts=1
    final var result = policy.decideResponse(20, 2, 0, POINTS_TO_WIN,
        new PendingEnvidoOutcome(2, 2, 1));
    assertThat(result).isEqualTo(BotEnvidoResponse.NO_QUIERO);
  }

  @Test
  void decideResponse_p5_rejectionGivesRivalWin_botSafe_quiero() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ONE);
    // chain=[envido]: ptsIfBotWins=2, ptsIfRivalWins=2, rejectedPts=1
    final var result = policy.decideResponse(10, 0, 2, POINTS_TO_WIN,
        new PendingEnvidoOutcome(2, 2, 1));
    assertThat(result).isEqualTo(BotEnvidoResponse.QUIERO);
  }

  @Test
  void decideResponse_faltaAt2_2_quiero() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ONE);
    // chain=[faltaEnvido(3,2,2)]: ptsIfBotWins=1, ptsIfRivalWins=1, rejectedPts=1
    final var result = policy.decideResponse(20, 2, 2, POINTS_TO_WIN,
        new PendingEnvidoOutcome(1, 1, 1));
    assertThat(result).isEqualTo(BotEnvidoResponse.QUIERO);
  }

  @Test
  void decideResponse_withoutPendingOutcome_throws() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ONE);

    assertThatThrownBy(() -> policy.decideResponse(20, 0, 0, POINTS_TO_WIN, null))
        .isInstanceOf(PendingEnvidoCallRequiredException.class)
        .hasMessage("Cannot decide envido response without a pending envido call");
  }

  @Test
  void decideCall_atMatchPoint_goodEnvido_callsFalta() {

    final var falta = faltaEnvido(POINTS_TO_WIN, 2, 2);
    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ONE);
    final var result = policy.decideCall(List.of(envido(), falta), 30, 2, 2, POINTS_TO_WIN, false,
        true);
    assertThat(result).contains(falta);
  }

  @Test
  void decideCall_atMatchPoint_neverRaisesInChain() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ZERO);
    final var result = policy.decideCall(List.of(realEnvido()), 30, 2, 0, POINTS_TO_WIN, false,
        false);
    assertThat(result).isEmpty();
  }

  @Test
  void decideCall_usesDynamicPointsToWin() {

    final var dynamicFalta = faltaEnvido(POINTS_TO_WIN_FIVE, 4, 4);
    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ONE);
    final var result = policy.decideCall(List.of(envido(), dynamicFalta), 30, 4, 4,
        POINTS_TO_WIN_FIVE, false, true);
    assertThat(result).contains(dynamicFalta);
  }

  @Test
  void decideCall_highRealEnvidoAtOneOne_skipsSuicidalCall() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ZERO);
    final var result = policy.decideCall(List.of(realEnvido()), 30, 1, 1, POINTS_TO_WIN, false,
        true);
    assertThat(result).isEmpty();
  }

  @Test
  void decideCall_raiseUsesProjectedTotalsAndSkipsSuicidalRaise() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ZERO);
    final var projectedRealRaise = call(5, 5, 2, BotEnvidoLevel.REAL_ENVIDO);
    final var result = policy.decideCall(List.of(projectedRealRaise), 30, 1, 1, POINTS_TO_WIN,
        false, false);
    assertThat(result).isEmpty();
  }

  @Test
  void decideCall_lowEnvidoAtTwoTwo_keepsTrapCall() {

    final var policy = new EnvidoDecisionPolicy(NEUTRAL, ALWAYS_ZERO);
    final var result = policy.decideCall(List.of(envido()), 15, 2, 2, POINTS_TO_WIN, false, true);
    assertThat(result).contains(envido());
  }

}
