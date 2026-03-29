package com.villo.truco.domain.model.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.EnvidoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.TrucoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BotDecisionEngineTest {

  private static final int POINTS_TO_WIN = 3;
  private static final int POINTS_TO_WIN_FIVE = 5;

  private static final BotPersonality AGGRESSIVE = new BotPersonality(100, 1, 100, 100, 50);
  private static final BotPersonality PASSIVE = new BotPersonality(1, 1, 1, 1, 1);

  private static final BotCard ANCHO_ESPADA = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard CUATRO_COPA = new BotCard(1, Card.of(Suit.COPA, 4));

  private static final BotTrucoCall TRUCO_CALL = new BotTrucoCall(2, 1);
  private static final BotTrucoCall RETRUCO_CALL = new BotTrucoCall(3, 2);

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

  private static BotEnvidoCall envido() {

    return new BotEnvidoCall(2, 2, 1, BotEnvidoLevel.ENVIDO);
  }

  private static BotMatchView playOnly(final List<BotCard> cards) {

    final var game = new GameContext(cards, 0, 0, null, 0, 0, false, true, false, POINTS_TO_WIN);
    final var truco = new TrucoContext(null, List.of(), null);
    final var envido = new EnvidoContext(List.of(), List.of(), List.of(), null);
    return new BotMatchView(game, truco, envido);
  }

  private static BotMatchView withTrucoResponse(final List<BotCard> cards, final int myScore,
      final int rivalScore, final BotTrucoCall currentOffer,
      final List<BotTrucoResponse> availableResponses, final BotTrucoCall availableTrucoCall,
      final List<BotEnvidoCall> availableEnvidoCalls, final int handsPlayed) {

    return withTrucoResponse(cards, myScore, rivalScore, currentOffer, availableResponses,
        availableTrucoCall, availableEnvidoCalls, handsPlayed, POINTS_TO_WIN);
  }

  private static BotMatchView withTrucoResponse(final List<BotCard> cards, final int myScore,
      final int rivalScore, final BotTrucoCall currentOffer,
      final List<BotTrucoResponse> availableResponses, final BotTrucoCall availableTrucoCall,
      final List<BotEnvidoCall> availableEnvidoCalls, final int handsPlayed,
      final int pointsToWin) {

    final var game = new GameContext(cards, myScore, rivalScore, null, 0, handsPlayed, false, false,
        false, pointsToWin);
    final var truco = new TrucoContext(availableTrucoCall, availableResponses, currentOffer);
    final var envido = new EnvidoContext(availableEnvidoCalls, List.of(), List.of(), null);
    return new BotMatchView(game, truco, envido);
  }

  @Test
  void decide_qymvam_takesOverEvenWhenEnvidoAvailable() {

    final var engine = new BotDecisionEngine(AGGRESSIVE, ALWAYS_ZERO);
    final var view = withTrucoResponse(List.of(ANCHO_ESPADA), 0, 2, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO,
            BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO), null, List.of(envido()), 0);
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.RespondTruco.class);
    assertThat(((BotAction.RespondTruco) action).response()).isEqualTo(
        BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO);
  }

  @Test
  void decide_noQuieroKillsRival_returnsNoQuiero() {

    final var engine = new BotDecisionEngine(AGGRESSIVE, ALWAYS_ZERO);
    final var view = withTrucoResponse(List.of(), 0, 2, RETRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO), null, List.of(), 0);
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.RespondTruco.class);
    assertThat(((BotAction.RespondTruco) action).response()).isEqualTo(BotTrucoResponse.NO_QUIERO);
  }

  @Test
  void decide_envidoAntesDeTruco_whenEnvidoAvailableWhileRespondingTruco() {

    final var engine = new BotDecisionEngine(AGGRESSIVE, ALWAYS_ZERO);
    final var view = withTrucoResponse(List.of(CUATRO_COPA), 0, 0, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO), null, List.of(envido()), 0);
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.CallEnvido.class);
  }

  @Test
  void decide_noEnvidoAntesDeTruco_whenEnvidoNotAvailable() {

    final var engine = new BotDecisionEngine(AGGRESSIVE, ALWAYS_ZERO);
    final var view = withTrucoResponse(List.of(ANCHO_ESPADA), 0, 0, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO), null, List.of(), 1);
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.RespondTruco.class);
  }

  @Test
  void decide_qymvamWithoutCards_acceptsTruco() {

    final var engine = new BotDecisionEngine(AGGRESSIVE, ALWAYS_ONE);
    final var view = withTrucoResponse(List.of(), 0, 2, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO), null, List.of(), 0);
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.RespondTruco.class);
    assertThat(((BotAction.RespondTruco) action).response()).isEqualTo(BotTrucoResponse.QUIERO);
  }

  @Test
  void decide_acceptsWhenRejectWouldGiveRivalExactWin() {

    final var engine = new BotDecisionEngine(PASSIVE, ALWAYS_ONE);
    final var view = withTrucoResponse(List.of(), 0, 1, RETRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO), null, List.of(), 0);
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.RespondTruco.class);
    assertThat(((BotAction.RespondTruco) action).response()).isEqualTo(BotTrucoResponse.QUIERO);
  }

  @Test
  void decide_escalatesTrucoWhenResponding() {

    final var engine = new BotDecisionEngine(AGGRESSIVE, ALWAYS_ZERO);
    final var view = withTrucoResponse(List.of(ANCHO_ESPADA), 0, 0, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO), RETRUCO_CALL,
        List.of(), 0);
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.CallTruco.class);
    assertThat(((BotAction.CallTruco) action).call()).isEqualTo(RETRUCO_CALL);
  }

  @Test
  void decide_fallsBackToPlayCard_whenNoSpecialActions() {

    final var engine = new BotDecisionEngine(PASSIVE, ALWAYS_ONE);
    final var view = playOnly(List.of(ANCHO_ESPADA));
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.PlayCard.class);
  }

  @Test
  void decide_usesDynamicPointsToWinFromGameContext() {

    final var engine = new BotDecisionEngine(AGGRESSIVE, ALWAYS_ZERO);
    final var view = withTrucoResponse(List.of(ANCHO_ESPADA), 0, 2, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO), null, List.of(), 0,
        POINTS_TO_WIN_FIVE);
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.RespondTruco.class);
    assertThat(((BotAction.RespondTruco) action).response()).isEqualTo(BotTrucoResponse.QUIERO);
  }

}
