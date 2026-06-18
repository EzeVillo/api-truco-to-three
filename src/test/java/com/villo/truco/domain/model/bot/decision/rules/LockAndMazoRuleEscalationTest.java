package com.villo.truco.domain.model.bot.decision.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.decision.CardLockAnalyzer;
import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.MatchArithmetic;
import com.villo.truco.domain.model.bot.decision.TantoProbabilityProvider;
import com.villo.truco.domain.model.bot.decision.UnplayedHandProbability;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.EnvidoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.TrucoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests para la lógica de escalada de cantos en LockAndMazoRule (US3, Caso 7).
 *
 * <p>Cuando el bot puede garantizar la victoria si el rival rechaza un canto (botWinsIfRejected),
 * la regla escala aunque aceptar haría que el bot se pasara de 3; en ese caso el bot se irá al mazo
 * después de que el rival acepte (lógica de encierro).
 */
class LockAndMazoRuleEscalationTest {

  private static final int POINTS_TO_WIN = 3;

  private static final BotCard ANCHO_ESPADA = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard CUATRO_COPA = new BotCard(1, Card.of(Suit.COPA, 4));

  // Vale cuatro: stakeIfAccepted=4, stakeIfRejected=3
  private static final BotTrucoCall VALE_CUATRO_CALL = new BotTrucoCall(4, 3);
  // Retruco: stakeIfAccepted=3, stakeIfRejected=2
  private static final BotTrucoCall RETRUCO_CALL = new BotTrucoCall(3, 2);
  // Truco: stakeIfAccepted=2, stakeIfRejected=1
  private static final BotTrucoCall TRUCO_CALL = new BotTrucoCall(2, 1);

  private static DecisionContext ctx(final BotMatchView view) {

    final var game = view.game();
    final var arithmetic = new MatchArithmetic(game.myScore(), game.rivalScore(),
        game.pointsToWin());
    final var lock = new CardLockAnalyzer(game);
    final var tanto = TantoProbabilityProvider.withKnownProbability(0.5);
    final var unplayed = new UnplayedHandProbability(game.myCards(), game.rivalCardPlayed());
    return new DecisionContext(view, arithmetic, lock, tanto, unplayed);
  }

  private static BotMatchView gameConLlamadaDisponible(final int myScore, final int rivalScore,
      final BotTrucoCall available) {

    final var game = new GameContext(List.of(ANCHO_ESPADA), myScore, rivalScore, null, 0, 1, false,
        true, true, false, POINTS_TO_WIN, 1);
    return new BotMatchView(game, new TrucoContext(available, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
  }

  // ----- Caso 7: escalar cuando botWinsIfRejected, aunque botBustsIfAccepted -----

  @Test
  void a00_llama_vale_cuatro_cuando_rechazo_gana_el_match() {

    // 0-0, vale cuatro: stakeIfRejected=3 → myScore(0)+3==3 → si rival rechaza, bot gana
    // Aunque stakeIfAccepted=4 → 0+4>3 (bust), el bot puede foldearse al mazo si rival acepta
    final var result = new LockAndMazoRule().apply(
        ctx(gameConLlamadaDisponible(0, 0, VALE_CUATRO_CALL)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.CallTruco.class);
    assertThat(((BotAction.CallTruco) result.get()).call()).isEqualTo(VALE_CUATRO_CALL);
  }

  @Test
  void a10_llama_retruco_cuando_rechazo_gana_el_match() {

    // 1-0 (bot gana 1), retruco: stakeIfRejected=2 → 1+2==3 → si rival rechaza, bot gana
    final var result = new LockAndMazoRule().apply(
        ctx(gameConLlamadaDisponible(1, 0, RETRUCO_CALL)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.CallTruco.class);
    assertThat(((BotAction.CallTruco) result.get()).call()).isEqualTo(RETRUCO_CALL);
  }

  @Test
  void a20_llama_truco_cuando_rechazo_gana_el_match() {

    // 2-0 (bot va a ganar), truco: stakeIfRejected=1 → 2+1==3 → si rival rechaza, bot gana
    final var result = new LockAndMazoRule().apply(ctx(gameConLlamadaDisponible(2, 0, TRUCO_CALL)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.CallTruco.class);
  }

  @Test
  void no_escala_cuando_rechazo_no_gana_el_match() {

    // 0-0, truco simple: stakeIfRejected=1 → 0+1=1 ≠ 3 → rechazo no gana; no escala por esta razón
    // (la regla de encierro de cartas manejaría un escenario diferente)
    final var result = new LockAndMazoRule().apply(ctx(gameConLlamadaDisponible(0, 0, TRUCO_CALL)));
    // Solo dispara en el path de lock de cartas o escalada; si no hay lock activo → empty
    assertThat(result).isEmpty();
  }

  @Test
  void no_escala_cuando_no_hay_llamada_disponible() {

    // Sin llamada disponible (null) no puede escalar
    final var game = new GameContext(List.of(ANCHO_ESPADA), 0, 0, null, 0, 1, false, true, false,
        false, POINTS_TO_WIN, 1);
    final var view = new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
    assertThat(new LockAndMazoRule().apply(ctx(view))).isEmpty();
  }

  @Test
  void no_acepta_truco_del_rival_fuera_de_match_point_cede_al_fallback() {

    // 0-0 (nadie a punto de ganar): la aceptación por probabilidad de encierro no aplica;
    // LockAndMazoRule cede (empty) para que el fallback de VE decida (p. ej. escalar truco).
    final var game = new GameContext(List.of(ANCHO_ESPADA), 0, 0, null, 0, 1, false, true, false,
        false, POINTS_TO_WIN, 1);
    final var view = new BotMatchView(game, new TrucoContext(VALE_CUATRO_CALL,
        List.of(com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse.QUIERO,
            com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse.NO_QUIERO), TRUCO_CALL),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
    assertThat(new LockAndMazoRule().apply(ctx(view))).isEmpty();
  }

}
