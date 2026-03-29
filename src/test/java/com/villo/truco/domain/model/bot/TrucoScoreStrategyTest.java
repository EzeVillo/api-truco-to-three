package com.villo.truco.domain.model.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import org.junit.jupiter.api.Test;

class TrucoScoreStrategyTest {

  private static final int POINTS_TO_WIN = 3;
  private static final int POINTS_TO_WIN_FIVE = 5;

  private static final BotTrucoCall TRUCO = new BotTrucoCall(2, 1);
  private static final BotTrucoCall RETRUCO = new BotTrucoCall(3, 2);
  private static final BotTrucoCall VALE_CUATRO = new BotTrucoCall(4, 3);

  @Test
  void qymvam_trueWhenRivalAt2AndTrucoAccepts() {

    assertThat(TrucoScoreStrategy.shouldQYMVAM(2, TRUCO, POINTS_TO_WIN)).isTrue();
  }

  @Test
  void qymvam_falseWhenRivalAt1AndTrucoAccepts() {

    assertThat(TrucoScoreStrategy.shouldQYMVAM(1, TRUCO, POINTS_TO_WIN)).isFalse();
  }

  @Test
  void qymvam_trueWhenRivalAt1AndRetrucoAccepts() {

    assertThat(TrucoScoreStrategy.shouldQYMVAM(1, RETRUCO, POINTS_TO_WIN)).isTrue();
  }

  @Test
  void qymvam_usesDynamicPointsToWin() {

    assertThat(TrucoScoreStrategy.shouldQYMVAM(2, TRUCO, POINTS_TO_WIN_FIVE)).isFalse();
  }

  @Test
  void rivalWinsIfRejected_trueWhenRivalAt2AndTrucoRejected() {

    assertThat(TrucoScoreStrategy.rivalWinsIfRejected(2, TRUCO, POINTS_TO_WIN)).isTrue();
  }

  @Test
  void rivalWinsIfRejected_falseWhenRivalAt1AndTrucoRejected() {

    assertThat(TrucoScoreStrategy.rivalWinsIfRejected(1, TRUCO, POINTS_TO_WIN)).isFalse();
  }

  @Test
  void botWinsIfRejected_trueWhenBotAt2AndTrucoRejected() {

    assertThat(TrucoScoreStrategy.botWinsIfRejected(2, TRUCO, POINTS_TO_WIN)).isTrue();
  }

  @Test
  void botWinsIfRejected_falseWhenBotWouldExceedAfterRejected() {

    assertThat(TrucoScoreStrategy.botWinsIfRejected(2, RETRUCO, POINTS_TO_WIN)).isFalse();
  }

  @Test
  void rivalExceedsIfAccepted_trueWhenRivalAt2AndTrucoAccepted() {

    assertThat(TrucoScoreStrategy.rivalExceedsIfAccepted(2, TRUCO, POINTS_TO_WIN)).isTrue();
  }

  @Test
  void rivalExceedsIfAccepted_falseWhenRivalAt1AndTrucoAccepted() {

    assertThat(TrucoScoreStrategy.rivalExceedsIfAccepted(1, TRUCO, POINTS_TO_WIN)).isFalse();
  }

  @Test
  void botExceedsIfAccepted_trueWhenBotAt2AndTrucoAccepts() {

    assertThat(TrucoScoreStrategy.botExceedsIfAccepted(2, TRUCO, POINTS_TO_WIN)).isTrue();
  }

  @Test
  void botExceedsIfAccepted_falseWhenBotAt1AndTrucoAccepts() {

    assertThat(TrucoScoreStrategy.botExceedsIfAccepted(1, TRUCO, POINTS_TO_WIN)).isFalse();
  }

  @Test
  void noQuieroKillsRival_trueWhenRivalAt2AndRetrucoRejected() {

    assertThat(TrucoScoreStrategy.noQuieroKillsRival(2, RETRUCO, POINTS_TO_WIN)).isTrue();
  }

  @Test
  void noQuieroKillsRival_falseWhenRivalAt1AndTrucoRejected() {

    assertThat(TrucoScoreStrategy.noQuieroKillsRival(1, TRUCO, POINTS_TO_WIN)).isFalse();
  }

  @Test
  void noQuieroKillsRival_falseWhenRivalAt2AndTrucoRejected() {

    assertThat(TrucoScoreStrategy.noQuieroKillsRival(2, TRUCO, POINTS_TO_WIN)).isFalse();
  }

  @Test
  void noQuieroKillsRival_trueWhenRivalAt2AndValeCuatroRejected() {

    assertThat(TrucoScoreStrategy.noQuieroKillsRival(2, VALE_CUATRO, POINTS_TO_WIN)).isTrue();
  }

}
