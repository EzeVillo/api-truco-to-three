package com.villo.truco.domain.model.bot.decision.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.EnvidoScoring;
import com.villo.truco.domain.model.bot.decision.CardLockAnalyzer;
import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.MatchArithmetic;
import com.villo.truco.domain.model.bot.decision.TantoProbabilityProvider;
import com.villo.truco.domain.model.bot.decision.UnplayedHandProbability;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.EnvidoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.TrucoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import com.villo.truco.domain.model.match.CardEvaluationService;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ExpectedValueFallbackRuleTest {

  private static final int POINTS_TO_WIN = 3;
  private static final BotPersonality PASSIVE = new BotPersonality(1, 1, 1, 1, 1);
  private static final EnvidoScoring SCORING = CardEvaluationService::envidoScore;

  private static final BotCard ANCHO = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard CUATRO = new BotCard(1, Card.of(Suit.COPA, 4));

  private static final BotTrucoCall TRUCO = new BotTrucoCall(2, 1);
  private static final Random ALWAYS_ONE = new Random() {
    @Override
    public double nextDouble() {
      return 1.0;
    }
  };

  private static DecisionContext ctx(final BotMatchView view) {

    final var game = view.game();
    final var arithmetic = new MatchArithmetic(game.myScore(), game.rivalScore(),
        game.pointsToWin());
    final var lock = new CardLockAnalyzer(game);
    final var tanto = TantoProbabilityProvider.withKnownProbability(0.5);
    final var unplayed = new UnplayedHandProbability(game.myCards(), game.rivalCardPlayed());
    return new DecisionContext(view, arithmetic, lock, tanto, unplayed);
  }

  private static BotMatchView soloJugarCarta(final List<BotCard> cards) {

    final var game = new GameContext(cards, 0, 0, null, 0, 0, false, true, false, false,
        POINTS_TO_WIN, cards.size());
    return new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
  }

  private static BotMatchView conRespuestaEnvido(final List<BotCard> cards,
      final BotMatchView.PendingEnvidoOutcome outcome) {

    final var game = new GameContext(cards, 0, 0, null, 15, 0, false, false, false, false,
        POINTS_TO_WIN, cards.size());
    return new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(), List.of(BotEnvidoResponse.QUIERO, BotEnvidoResponse.NO_QUIERO),
            List.of(), outcome));
  }

  @Test
  void siempreDevuelveAccionLegal_cuandoSoloSePuedeJugarCarta() {

    final var rule = new ExpectedValueFallbackRule(PASSIVE, ALWAYS_ONE, SCORING);
    final var result = rule.apply(ctx(soloJugarCarta(List.of(ANCHO))));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.PlayCard.class);
  }

  @Test
  void siempreDevuelveAccion_nuncaOptionalVacio() {

    final var rule = new ExpectedValueFallbackRule(PASSIVE, ALWAYS_ONE, SCORING);
    final var result = rule.apply(ctx(soloJugarCarta(List.of(CUATRO))));
    assertThat(result).isPresent();
  }

  @Test
  void personalidadModulaFaroles_conPersonalidadAgresivaYRandomCero_llamaTruco() {

    final var aggressive = new BotPersonality(100, 1, 100, 100, 50);
    final var alwaysZero = new Random() {
      @Override
      public double nextDouble() {
        return 0.0;
      }
    };
    final var rule = new ExpectedValueFallbackRule(aggressive, alwaysZero, SCORING);

    final var game = new GameContext(List.of(ANCHO), 0, 0, null, 0, 0, false, true, false, false,
        POINTS_TO_WIN, 1);
    final var view = new BotMatchView(game, new TrucoContext(TRUCO, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));

    final var result = rule.apply(ctx(view));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.CallTruco.class);
  }

  @Test
  void nuncaQYMVAMSinCartas_cuandoBotSinCartasDevuelveRespuestaTruco() {

    final var rule = new ExpectedValueFallbackRule(PASSIVE, ALWAYS_ONE, SCORING);
    // bot sin cartas: las respuestas disponibles no incluyen QYMVAM
    final var game = new GameContext(List.of(), 0, 0, null, 0, 0, false, false, false, false,
        POINTS_TO_WIN, 1);
    final var view = new BotMatchView(game,
        new TrucoContext(null, List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO),
            TRUCO), new EnvidoContext(List.of(), List.of(), List.of(), null));

    final var result = rule.apply(ctx(view));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.RespondTruco.class);
    final var resp = ((BotAction.RespondTruco) result.get()).response();
    assertThat(resp).isNotEqualTo(BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO);
  }

  @Test
  void respondeEnvido_cuandoDebeResponder() {

    final var rule = new ExpectedValueFallbackRule(PASSIVE, ALWAYS_ONE, SCORING);
    final var outcome = new BotMatchView.PendingEnvidoOutcome(2, 2, 1);
    final var result = rule.apply(ctx(conRespuestaEnvido(List.of(CUATRO), outcome)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.RespondEnvido.class);
  }

  @Test
  void prioridad_esMuyAlta_indicaQueEsElUltimoEnEvaluarse() {

    final var rule = new ExpectedValueFallbackRule(PASSIVE, ALWAYS_ONE, SCORING);
    assertThat(rule.priority()).isGreaterThan(100);
  }

}
