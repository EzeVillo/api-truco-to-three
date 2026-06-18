package com.villo.truco.domain.model.bot.decision.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.decision.CardLockAnalyzer;
import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.MatchArithmetic;
import com.villo.truco.domain.model.bot.decision.TantoProbabilityProvider;
import com.villo.truco.domain.model.bot.decision.UnplayedHandProbability;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.EnvidoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.TrucoContext;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForceRivalBustRuleTest {

  private static final int POINTS_TO_WIN = 3;
  private static final BotCard CUATRO = new BotCard(1, Card.of(Suit.COPA, 4));
  private static final BotCard ANCHO = new BotCard(14, Card.of(Suit.ESPADA, 1));

  // real-envido: accepted=3 para quien gana, rejected=1
  // Con rival en 1: si acepta y gana → 1+3=4 > 3 → busts!
  private static final BotEnvidoCall REAL_ENVIDO_CALL = new BotEnvidoCall(3, 3, 1,
      BotEnvidoLevel.REAL_ENVIDO);
  // envido: accepted=2, rejected=1. Con rival en 1: 1+2=3 → NO bust
  private static final BotEnvidoCall ENVIDO_CALL = new BotEnvidoCall(2, 2, 1,
      BotEnvidoLevel.ENVIDO);

  private static DecisionContext ctx(final BotMatchView view, final double winProbability) {

    final var game = view.game();
    final var arithmetic = new MatchArithmetic(game.myScore(), game.rivalScore(),
        game.pointsToWin());
    final var lock = new CardLockAnalyzer(game);
    final var tanto = TantoProbabilityProvider.withKnownProbability(winProbability);
    final var unplayed = new UnplayedHandProbability(game.myCards(), game.rivalCardPlayed());
    return new DecisionContext(view, arithmetic, lock, tanto, unplayed);
  }

  private static BotMatchView scenarioBotArriba(final List<BotCard> cards,
      final List<BotEnvidoCall> envidoCalls) {

    // Bot=2, rival=1 (bot arriba 2-1), probabilidad de ganar tanto muy baja
    final var game = new GameContext(cards, 2, 1, null, 0, 0, false, true, false, false,
        POINTS_TO_WIN, cards.size());
    return new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(envidoCalls, List.of(), List.of(), null));
  }

  @Test
  void botArriba2_1_probPerdertantoMuyAlta_llamaEnvidoQueHaceRivalSePase() {

    // prob ganar = 0.10 → prob perder = 0.90 > 0.70; real-envido hace busts al rival si acepta
    final var view = scenarioBotArriba(List.of(CUATRO), List.of(REAL_ENVIDO_CALL));
    final var result = new ForceRivalBustRule().apply(ctx(view, 0.10));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.CallEnvido.class);
    final var call = ((BotAction.CallEnvido) result.get()).call();
    assertThat(call.level()).isEqualTo(BotEnvidoLevel.REAL_ENVIDO);
  }

  @Test
  void umbralNoSuperado_probabilidadPerdidaModerada_noOpina() {

    // prob ganar = 0.40 → prob perder = 0.60 < 0.70 → no llega al umbral
    final var view = scenarioBotArriba(List.of(CUATRO), List.of(REAL_ENVIDO_CALL));
    final var result = new ForceRivalBustRule().apply(ctx(view, 0.40));
    assertThat(result).isEmpty();
  }

  @Test
  void sinLlamadaQueHagaRivalPasarse_noOpina() {

    // Solo ENVIDO disponible: con rival en 1, ENVIDO accepted=2 → 1+2=3 NO excede 3
    final var view = scenarioBotArriba(List.of(CUATRO), List.of(ENVIDO_CALL));
    final var result = new ForceRivalBustRule().apply(ctx(view, 0.10));
    assertThat(result).isEmpty();
  }

  @Test
  void cuandoEnvidoNoPuedeSerLlamado_noOpina() {

    final var view = scenarioBotArriba(List.of(CUATRO), List.of());
    final var result = new ForceRivalBustRule().apply(ctx(view, 0.05));
    assertThat(result).isEmpty();
  }

  @Test
  void cuandoEnvidoMustRespond_noOpina() {

    // El bot está respondiendo un envido rival: esta regla no debe interferir
    final var game = new GameContext(List.of(CUATRO), 2, 1, null, 0, 0, false, false, false, false,
        POINTS_TO_WIN, 1);
    final var view = new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(REAL_ENVIDO_CALL),
            List.of(com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse.QUIERO), List.of(),
            new BotMatchView.PendingEnvidoOutcome(3, 3, 1)));
    final var result = new ForceRivalBustRule().apply(ctx(view, 0.05));
    assertThat(result).isEmpty();
  }

  @Test
  void umbralEsConfigurableComoConstante_valorInicial0punto70() {

    // exactamente en el umbral: prob ganar = 0.30 → prob perder = 0.70 (igual al umbral, NO supera)
    final var view = scenarioBotArriba(List.of(CUATRO), List.of(REAL_ENVIDO_CALL));
    assertThat(new ForceRivalBustRule().apply(ctx(view, 0.30))).isEmpty();
    // prob ganar = 0.29 → prob perder = 0.71 > 0.70 → sí activa
    assertThat(new ForceRivalBustRule().apply(ctx(view, 0.29))).isPresent();
  }

}
