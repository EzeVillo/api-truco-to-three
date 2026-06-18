package com.villo.truco.domain.model.bot.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.BotDecisionEngine;
import com.villo.truco.domain.model.bot.EnvidoScoring;
import com.villo.truco.domain.model.bot.decision.rules.EnvidoAtTwoTwoRule;
import com.villo.truco.domain.model.bot.decision.rules.ExpectedValueFallbackRule;
import com.villo.truco.domain.model.bot.decision.rules.ForceRivalBustRule;
import com.villo.truco.domain.model.bot.decision.rules.LockAndMazoRule;
import com.villo.truco.domain.model.bot.decision.rules.ResponseToRivalCallRule;
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
import com.villo.truco.domain.model.match.CardEvaluationService;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validación del quickstart (T040): confirmar que agregar una casuística nueva (p. ej. 2-0) es
 * aditivo — nueva {@link DecisionRule} + registro en el {@link DecisionRuleRegistry}, sin editar
 * reglas existentes.
 *
 * <p>Pasos del quickstart validados:
 * <ol>
 *   <li>Verificar si la casuística ya está cubierta por la aritmética de las reglas existentes.
 *   <li>Si se necesita una táctica nueva, crearla y registrarla con su {@code priority()}.
 *   <li>No editar reglas existentes: las posiciones que ya resolvían siguen igual.
 * </ol>
 *
 * <p>La regla de prueba {@link TwoZeroRule} (anidada, prioridad 35) se registra junto a las 5 reglas
 * existentes en un registry construido en el test; ninguna regla existente se modifica.
 */
@DisplayName("Quickstart — extensión aditiva con casuística 2-0")
class QuickstartAdditivityTest {

  private static final int POINTS_TO_WIN = 3;
  private static final BotPersonality PERSONALITY = new BotPersonality(50, 50, 50, 50, 50);
  private static final EnvidoScoring SCORING = CardEvaluationService::envidoScore;

  private static final BotCard ANCHO_ESPADA = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard CUATRO_COPA = new BotCard(1, Card.of(Suit.COPA, 4));

  private static final BotTrucoCall TRUCO_CALL = new BotTrucoCall(2, 1);
  private static final BotEnvidoCall ENVIDO = new BotEnvidoCall(2, 2, 1, BotEnvidoLevel.ENVIDO);
  private static final BotEnvidoCall FALTA_ENVIDO = new BotEnvidoCall(2, 2, 1,
      BotEnvidoLevel.FALTA_ENVIDO);

  private static DecisionContext ctx(final BotMatchView view) {

    final var game = view.game();
    final var arithmetic = new MatchArithmetic(game.myScore(), game.rivalScore(),
        game.pointsToWin());
    final var lock = new CardLockAnalyzer(game);
    final var tanto = TantoProbabilityProvider.withKnownProbability(0.1);
    final var unplayed = new UnplayedHandProbability(game.myCards(), game.rivalCardPlayed());
    return new DecisionContext(view, arithmetic, lock, tanto, unplayed);
  }

  private static BotMatchView posicion(final List<BotCard> cards, final int my, final int rival,
      final BotTrucoCall availableTruco, final List<BotTrucoResponse> responses,
      final List<BotEnvidoCall> envidoCalls) {

    final var game = new GameContext(cards, my, rival, null, 0, 0, false, true, false, false,
        POINTS_TO_WIN, 1);
    final var truco = new TrucoContext(availableTruco, responses, null);
    final var envido = new EnvidoContext(envidoCalls, List.of(), List.of(), null);
    return new BotMatchView(game, truco, envido);
  }

  /**
   * Registry con las 5 reglas existentes + la regla nueva (aditivo, sin editar existentes).
   */
  private static DecisionRuleRegistry registryConReglaNueva(final DecisionRule nueva) {

    return new DecisionRuleRegistry(
        List.of(new ResponseToRivalCallRule(), new EnvidoAtTwoTwoRule(), new ForceRivalBustRule(),
            new LockAndMazoRule(),
            new ExpectedValueFallbackRule(PERSONALITY, new java.util.Random(), SCORING), nueva));
  }

  @Test
  @DisplayName("2-0 ya está cubierta por la aritmética de las reglas existentes")
  void casuistica_2_0_ya_cubierta_por_el_motor_existente() {

    // Quickstart paso 1: verificar si ya está cubierta. A 2-0 con carta alta, el motor actual
    // produce una jugada legal (aquí CallTruco vía la escalada de LockAndMazoRule).
    final var engine = new BotDecisionEngine(PERSONALITY, SCORING);
    final var view = posicion(List.of(ANCHO_ESPADA, CUATRO_COPA), 2, 0, TRUCO_CALL, List.of(),
        List.of());
    final var action = engine.decide(view);
    assertThat(action).isInstanceOf(BotAction.CallTruco.class);
  }

  @Test
  @DisplayName("una nueva TwoZeroRule se registra additivamente y sombrea el comportamiento a 2-0")
  void casuistica_2_0_es_aditiva_nueva_regla_sombrea_sin_editar_existentes() {

    final var registry = registryConReglaNueva(new TwoZeroRule());

    // A 2-0: TwoZeroRule (prioridad 35) se evalúa antes que LockAndMazoRule (40) y sombrea su
    // CallTruco con una jugada de carta segura (no autobustarse cantando truco a 2-0).
    final var vista20 = posicion(List.of(ANCHO_ESPADA, CUATRO_COPA), 2, 0, TRUCO_CALL, List.of(),
        List.of());
    final var accion20 = registry.decide(ctx(vista20));
    assertThat(accion20).isInstanceOf(BotAction.PlayCard.class);
    assertThat(((BotAction.PlayCard) accion20).card()).isEqualTo(CUATRO_COPA);
  }

  @Test
  @DisplayName("la nueva regla no altera las posiciones que las reglas existentes ya resolvían")
  void casuistica_2_0_no_edita_reglas_existentes_las_posiciones_previas_quedan_iguales() {

    final var registry = registryConReglaNueva(new TwoZeroRule());

    // 2-2 con tanto bajo → EnvidoAtTwoTwoRule (prioridad 20) sigue decidiendo ENVIDO.
    final var vista22 = posicion(List.of(CUATRO_COPA), 2, 2, null, List.of(),
        List.of(ENVIDO, FALTA_ENVIDO));
    final var accion22 = registry.decide(ctx(vista22));
    assertThat(accion22).isInstanceOf(BotAction.CallEnvido.class);
    assertThat(((BotAction.CallEnvido) accion22).call().level()).isEqualTo(BotEnvidoLevel.ENVIDO);

    // 0-2 respondiendo truco con QYMVAM → ResponseToRivalCallRule (prioridad 10) sigue QYMVAM.
    final var vista02 = posicion(List.of(ANCHO_ESPADA), 0, 2, null,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO,
            BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO), List.of());
    final var truco = new TrucoContext(null,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO,
            BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO), TRUCO_CALL);
    final var game02 = new GameContext(List.of(ANCHO_ESPADA), 0, 2, null, 0, 0, false, false, false,
        false, POINTS_TO_WIN, 1);
    final var accion02 = registry.decide(ctx(new BotMatchView(game02, truco,
        new EnvidoContext(List.of(), List.of(), List.of(), null))));
    assertThat(accion02).isInstanceOf(BotAction.RespondTruco.class);
    assertThat(((BotAction.RespondTruco) accion02).response()).isEqualTo(
        BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO);
  }

  /**
   * Regla de prueba (casuística 2-0). A 2-0 (bot a un punto del triunfo, rival en 0) con carta
   * jugable, juega la carta más baja en lugar de cantar truco (que lo autobustaría si el rival
   * acepta y el bot gana la mano). Prioridad 35: entre ForceRivalBustRule (30) y LockAndMazoRule
   * (40), de modo que sombrea la escalada de LockAndMazo a 2-0 sin editarla.
   */
  private static final class TwoZeroRule implements DecisionRule {

    private static final int PRIORITY = 35;

    @Override
    public Optional<BotAction> apply(final DecisionContext ctx) {

      final var game = ctx.view().game();
      final boolean dosACero = game.myScore() == game.pointsToWin() - 1 && game.rivalScore() == 0;
      if (!dosACero || !game.canPlayCard() || game.myCards().isEmpty()) {
        return Optional.empty();
      }
      final var lowest = game.myCards().stream().min(Comparator.comparingInt(BotCard::trucoRank))
          .orElseThrow();
      return Optional.of(new BotAction.PlayCard(lowest));
    }

    @Override
    public int priority() {

      return PRIORITY;
    }

    @Override
    public String name() {

      return "TwoZeroRule";
    }

  }

}
