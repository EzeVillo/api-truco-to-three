package com.villo.truco.domain.model.bot.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.BotDecisionEngine;
import com.villo.truco.domain.model.bot.EnvidoScoring;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.EnvidoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.PendingEnvidoOutcome;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.TrucoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import com.villo.truco.domain.model.match.CardEvaluationService;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests de regresión de los invariantes de valor esperado (SC-004) y del límite determinístico
 * (SC-005) del motor rediseñado.
 *
 * <p>SC-004: el motor nunca elige una jugada de valor esperado negativo (suicida), entendida como
 * una jugada donde todas las ramas plausibles llevan al bot a perder (p. ej. aceptar un truco que
 * hace que el bot se pase si gana la mano y que el rival llegue exacto si la pierde).
 *
 * <p>SC-005: las decisiones que solo dependen de información cierta se resuelven de forma
 * determinística y repetible (mismo input → misma salida), sin depender de Random.
 */
class ExpectedValueInvariantsTest {

  private static final int POINTS_TO_WIN = 3;
  private static final BotPersonality AGGRESSIVE = new BotPersonality(100, 1, 100, 100, 50);
  private static final BotPersonality PASSIVE = new BotPersonality(1, 1, 1, 1, 1);
  private static final EnvidoScoring SCORING = CardEvaluationService::envidoScore;

  private static final BotCard ANCHO_ESPADA = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard CUATRO_COPA = new BotCard(1, Card.of(Suit.COPA, 4));

  private static final BotTrucoCall TRUCO_CALL = new BotTrucoCall(2, 1);

  private static final BotEnvidoCall ENVIDO = new BotEnvidoCall(2, 2, 1, BotEnvidoLevel.ENVIDO);
  private static final BotEnvidoCall FALTA_ENVIDO = new BotEnvidoCall(2, 2, 1,
      BotEnvidoLevel.FALTA_ENVIDO);

  // ---------- SC-004: 0 jugadas de VE negativo ----------

  /**
   * Una acción es "VE negativa" (suicida) si todas sus ramas plausibles llevan al bot a perder:
   * aceptar truco/envido donde el bot se pasa si gana y el rival llega exacto si pierde, o cantar
   * envido donde el bot se pasa si gana el tanto, el rival llega exacto si lo gana y el bot se pasa
   * si el rival rechaza.
   */
  private static boolean esAccionSuicida(final BotAction action, final BotMatchView view) {

    final var game = view.game();
    final int my = game.myScore();
    final int rival = game.rivalScore();
    final int toWin = game.pointsToWin();

    if (action instanceof BotAction.RespondTruco(BotTrucoResponse response1) && (response1 == BotTrucoResponse.QUIERO
        || response1 == BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO)) {
      final int stake = view.truco().currentCall().stakeIfAccepted();
      final boolean botSePasaSiGana = my + stake > toWin;
      final boolean rivalLlegaExactoSiPierde = rival + stake == toWin;
      return botSePasaSiGana && rivalLlegaExactoSiPierde;
    }
    if (action instanceof BotAction.RespondEnvido(BotEnvidoResponse response)
        && response == BotEnvidoResponse.QUIERO) {
      final var outcome = view.envido().pendingOutcome();
      if (outcome == null) {
        return false;
      }
      final boolean botSePasaSiGana = my + outcome.acceptedPointsIfBotWins() > toWin;
      final boolean rivalLlegaExactoSiPierde = rival + outcome.acceptedPointsIfRivalWins() == toWin;
      return botSePasaSiGana && rivalLlegaExactoSiPierde;
    }
    if (action instanceof BotAction.CallEnvido(BotEnvidoCall c)) {
      final boolean botSePasaSiGana = my + c.acceptedPointsIfBotWins() > toWin;
      final boolean rivalLlegaExactoSiGana = rival + c.acceptedPointsIfRivalWins() == toWin;
      final boolean botSePasaSiRechazan = my + c.rejectedPointsIfRivalDeclines() > toWin;
      return botSePasaSiGana && rivalLlegaExactoSiGana && botSePasaSiRechazan;
    }
    return false;
  }

  private static BotMatchView respondiendoTruco(final List<BotCard> cards, final int my,
      final int rival, final BotTrucoCall current, final List<BotTrucoResponse> responses) {

    final var game = new GameContext(cards, my, rival, null, 0, 0, false, false, false, false,
        POINTS_TO_WIN, cards.size());
    final var truco = new TrucoContext(null, responses, current);
    return new BotMatchView(game, truco, new EnvidoContext(List.of(), List.of(), List.of(), null));
  }

  private static BotMatchView respondiendoEnvido(final List<BotCard> cards, final int my,
      final int rival, final int envidoScore, final PendingEnvidoOutcome outcome) {

    final var game = new GameContext(cards, my, rival, null, envidoScore, 0, false, false, false,
        false, POINTS_TO_WIN, cards.size());
    final var envido = new EnvidoContext(List.of(),
        List.of(BotEnvidoResponse.QUIERO, BotEnvidoResponse.NO_QUIERO), List.of(), outcome);
    return new BotMatchView(game, new TrucoContext(null, List.of(), null), envido);
  }

  @Test
  void sc004_ningunaJugadaAceptaTrucoSuicida_cuandoBotSePasariaSiGanaYRivalLlegaExactoSiPierde() {

    // 2-1 respondiendo al truco: aceptar → bot gana mano → 2+2=4 (se pasa); bot pierde mano →
    // rival 1+2=3 (gana exacto). Ambas ramas pierden → aceptar es VE negativo. El motor debe
    // rechazar (NO_QUIERO), nunca QUIERO/QYMVAM.
    final var engine = new BotDecisionEngine(AGGRESSIVE, SCORING);
    final var view = respondiendoTruco(List.of(ANCHO_ESPADA), 2, 1, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO,
            BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO));
    final var action = engine.decide(view);
    assertThat(esAccionSuicida(action, view)).as(
            "No debe aceptar un truco suicida (bot se pasa si gana, rival gana si bot pierde)")
        .isFalse();
    assertThat(action).isInstanceOf(BotAction.RespondTruco.class);
    assertThat(((BotAction.RespondTruco) action).response()).isEqualTo(BotTrucoResponse.NO_QUIERO);
  }

  @Test
  void sc004_ningunaJugadaAceptaEnvidoSuicida_cuandoBotSePasariaSiGanaElTanto() {

    // 2-1 respondiendo envido con outcome(2,2,1): aceptar → bot gana tanto → 2+2=4 (se pasa);
    // bot pierde tanto → rival 1+2=3 (gana exacto). Aceptar es VE negativo → debe NO_QUIERO.
    final var engine = new BotDecisionEngine(AGGRESSIVE, SCORING);
    final var outcome = new PendingEnvidoOutcome(2, 2, 1);
    final var view = respondiendoEnvido(List.of(ANCHO_ESPADA), 2, 1, 20, outcome);
    final var action = engine.decide(view);
    assertThat(esAccionSuicida(action, view)).as("No debe aceptar un envido suicida").isFalse();
    assertThat(action).isInstanceOf(BotAction.RespondEnvido.class);
    assertThat(((BotAction.RespondEnvido) action).response()).isEqualTo(
        BotEnvidoResponse.NO_QUIERO);
  }

  @Test
  void sc004_bateriaDePosiciones_ningunaProduceJugadaSuicida() {

    final var engine = new BotDecisionEngine(AGGRESSIVE, SCORING);
    final var positions = List.of(
        // 2-1 respondiendo truco (aceptar suicida)
        respondiendoTruco(List.of(ANCHO_ESPADA), 2, 1, TRUCO_CALL,
            List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO,
                BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO)),
        // 2-1 respondiendo envido (aceptar suicida)
        respondiendoEnvido(List.of(ANCHO_ESPADA), 2, 1, 20, new PendingEnvidoOutcome(2, 2, 1)),
        // 2-2 cantando envido (trampa: NO suicida, el rival se pasa si gana el tanto)
        new BotMatchView(
            new GameContext(List.of(CUATRO_COPA), 2, 2, null, 0, 0, false, true, false, false,
                POINTS_TO_WIN, 1), new TrucoContext(null, List.of(), null),
            new EnvidoContext(List.of(ENVIDO, FALTA_ENVIDO), List.of(), List.of(), null)),
        // 0-2 respondiendo truco con QYMVAM (rival se pasa si acepta) → QYMVAM (no suicida)
        respondiendoTruco(List.of(ANCHO_ESPADA), 0, 2, TRUCO_CALL,
            List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO,
                BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO)),
        // 1-2 respondiendo truco (aceptar lleva al bot a 3 exacto → no suicida)
        respondiendoTruco(List.of(ANCHO_ESPADA), 1, 2, TRUCO_CALL,
            List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO)),
        // 0-0 jugando carta (sin cantos forzados)
        new BotMatchView(
            new GameContext(List.of(ANCHO_ESPADA), 0, 0, null, 0, 0, false, true, false, false,
                POINTS_TO_WIN, 1), new TrucoContext(TRUCO_CALL, List.of(), null),
            new EnvidoContext(List.of(), List.of(), List.of(), null)));
    for (final var view : positions) {
      final var action = engine.decide(view);
      assertThat(esAccionSuicida(action, view)).as(
              "Posición %s -> acción %s no debe ser suicida (VE negativo)", view.game(), action)
          .isFalse();
    }
  }

  // ---------- SC-005: determinismo sin Random ----------

  @Test
  void sc005_reglasDeterministicas_mismoInput_mismaSalida_sinImportarRandom() {

    // Las posiciones resueltas por reglas determinísticas (ResponseToRivalCall, EnvidoAtTwoTwo,
    // ForceRivalBust, LockAndMazo) no consultan Random: dos motores independientes —cada uno con
    // su propio Random interno— deciden exactamente la misma acción.
    final var positions = List.of(
        // EnvidoAtTwoTwoRule: 2-2 tanto bajo → ENVIDO
        new BotMatchView(
            new GameContext(List.of(CUATRO_COPA), 2, 2, null, 0, 0, false, true, false, false,
                POINTS_TO_WIN, 1), new TrucoContext(null, List.of(), null),
            new EnvidoContext(List.of(ENVIDO, FALTA_ENVIDO), List.of(), List.of(), null)),
        // ResponseToRivalCallRule: 0-2 truco con QYMVAM → QYMVAM
        respondiendoTruco(List.of(ANCHO_ESPADA), 0, 2, TRUCO_CALL,
            List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO,
                BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO)),
        // LockAndMazoRule: 0-0 encierro al avanzar → PlayCard
        new BotMatchView(
            new GameContext(List.of(ANCHO_ESPADA, CUATRO_COPA), 0, 0, CUATRO_COPA, 0, 1, false,
                true, false, false, POINTS_TO_WIN, 0),
            new TrucoContext(TRUCO_CALL, List.of(), null),
            new EnvidoContext(List.of(), List.of(), List.of(), null)),
        // ForceRivalBustRule: 2-1 arriba tanto muy bajo → FALTA_ENVIDO (rival se pasa si gana)
        new BotMatchView(
            new GameContext(List.of(CUATRO_COPA), 2, 1, null, 0, 0, false, true, false, false,
                POINTS_TO_WIN, 1), new TrucoContext(null, List.of(), null),
            new EnvidoContext(List.of(new BotEnvidoCall(2, 3, 1, BotEnvidoLevel.FALTA_ENVIDO)),
                List.of(), List.of(), null)));
    for (final var view : positions) {
      final var withRandomA = new BotDecisionEngine(PASSIVE, SCORING).decide(view);
      final var withRandomB = new BotDecisionEngine(PASSIVE, SCORING).decide(view);
      assertThat(withRandomA).as("Decisión determinística independientemente de Random")
          .isEqualTo(withRandomB);
    }
  }

  @Test
  void sc005_mismaInstancia_repetible_mismoInputMismaSalida() {

    // Misma instancia del motor llamada dos veces con la misma vista → misma acción
    // (repetibilidad del resultado para decisiones de información cierta).
    final var engine = new BotDecisionEngine(PASSIVE, SCORING);
    final var view = new BotMatchView(
        new GameContext(List.of(CUATRO_COPA), 2, 2, null, 0, 0, false, true, false, false,
            POINTS_TO_WIN, 1), new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(ENVIDO, FALTA_ENVIDO), List.of(), List.of(), null));
    final var first = engine.decide(view);
    final var second = engine.decide(view);
    assertThat(second).isEqualTo(first);
  }

}
