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

class LockAndMazoRuleTest {

  private static final int POINTS_TO_WIN = 3;

  // Cartas: ANCHO_ESPADA rank=14, CUATRO_COPA rank=1
  private static final BotCard ANCHO_ESPADA = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard CUATRO_COPA = new BotCard(1, Card.of(Suit.COPA, 4));

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

  private static BotMatchView gameConEncierroAlAvanzar(final int myScore, final int rivalScore) {

    // Bot tiene 2 cartas, rival jugó carta débil (bot la puede matar), rival sin cartas en mano
    // → leadsToLockIfAdvance() = true
    final var game = new GameContext(List.of(ANCHO_ESPADA, CUATRO_COPA), myScore, rivalScore,
        CUATRO_COPA, 0, 1, false, true, false, false, POINTS_TO_WIN, 0);
    return new BotMatchView(game, new TrucoContext(TRUCO_CALL, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
  }

  private static BotMatchView gameTerceraManoSinMatar(final int myScore, final int rivalScore) {

    // 3ª mano: bot tiene 1 carta que NO mata la del rival, rival sin cartas → rival no puede QYMVAM
    final var game = new GameContext(List.of(CUATRO_COPA), myScore, rivalScore,
        ANCHO_ESPADA, 0, 2, false, true, false, false, POINTS_TO_WIN, 0);
    return new BotMatchView(game, new TrucoContext(TRUCO_CALL, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
  }

  private static BotMatchView gamePostTrucoAceptado(final int myScore, final int rivalScore) {

    // Truco ya aceptado: bot puede irse al mazo (canFold=true), rival sin cartas, bot no mata
    final var game = new GameContext(List.of(CUATRO_COPA), myScore, rivalScore,
        ANCHO_ESPADA, 0, 2, false, true, true, false, POINTS_TO_WIN, 0);
    return new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
  }

  // ----- leadsToLockIfAdvance: avanzar sin cantar -----

  @Test
  void avanza_sin_cantar_cuando_lideraEncierro_dosCartas() {

    // Bot tiene 2 cartas, puede matar la del rival, rival sin cartas → avanza (juega carta)
    final var result = new LockAndMazoRule().apply(ctx(gameConEncierroAlAvanzar(0, 0)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.PlayCard.class);
  }

  @Test
  void avanza_sin_cantar_aunque_truco_disponible_2a2() {

    // 2-2 con truco disponible: la regla no llama truco antes de tiempo, avanza
    final var result = new LockAndMazoRule().apply(ctx(gameConEncierroAlAvanzar(2, 2)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.PlayCard.class);
  }

  @Test
  void avanza_juega_la_carta_que_mata_al_rival() {

    // La carta jugada debe ser la que supera al rival (ANCHO_ESPADA sobre CUATRO_COPA)
    final var result = new LockAndMazoRule().apply(ctx(gameConEncierroAlAvanzar(0, 0)));
    assertThat(result).isPresent();
    final var played = ((BotAction.PlayCard) result.get()).card();
    assertThat(played.trucoRank()).isGreaterThan(CUATRO_COPA.trucoRank());
  }

  // ----- rivalIsOutOfCards && !botBeatsPlayedCard: cantar truco, rival no puede QYMVAM -----

  @Test
  void canta_truco_en_tercera_mano_cuando_no_mata_carta_rival() {

    // 3ª mano, bot no mata la del rival, rival sin cartas → llama truco (no puede QYMVAM)
    final var result = new LockAndMazoRule().apply(ctx(gameTerceraManoSinMatar(0, 0)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.CallTruco.class);
  }

  @Test
  void canta_truco_sin_importar_el_marcador_porque_rival_no_puede_qymvam() {

    // La razón para cantar truco no es el marcador sino que rival no puede QYMVAM
    final var result = new LockAndMazoRule().apply(ctx(gameTerceraManoSinMatar(1, 1)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.CallTruco.class);
  }

  @Test
  void no_canta_truco_si_no_hay_call_disponible() {

    // Sin truco disponible (null), la regla no puede cantar → no opina
    final var game = new GameContext(List.of(CUATRO_COPA), 0, 0, ANCHO_ESPADA, 0, 2,
        false, true, false, false, POINTS_TO_WIN, 0);
    final var view = new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
    assertThat(new LockAndMazoRule().apply(ctx(view))).isEmpty();
  }

  // ----- Fold después de truco aceptado -----

  @Test
  void se_va_al_mazo_cuando_truco_fue_aceptado_y_no_puede_ganar() {

    // Truco aceptado (canFold=true, no hay call disponible), rival sin cartas, bot no mata
    final var result = new LockAndMazoRule().apply(ctx(gamePostTrucoAceptado(0, 0)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.Fold.class);
  }

  @Test
  void se_va_al_mazo_en_2a2_despues_de_truco_aceptado() {

    final var result = new LockAndMazoRule().apply(ctx(gamePostTrucoAceptado(2, 2)));
    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(BotAction.Fold.class);
  }

  // ----- Casos sin encierro: la regla no opina -----

  @Test
  void no_opera_cuando_rival_todavia_tiene_cartas() {

    // Rival aún tiene carta: no hay encierro posible
    final var game = new GameContext(List.of(CUATRO_COPA), 0, 0, ANCHO_ESPADA, 0, 2,
        false, true, false, false, POINTS_TO_WIN, 1);
    final var view = new BotMatchView(game, new TrucoContext(TRUCO_CALL, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
    assertThat(new LockAndMazoRule().apply(ctx(view))).isEmpty();
  }

  @Test
  void no_opera_cuando_rival_no_ha_jugado_carta() {

    // Sin carta jugada del rival: CardLockAnalyzer.botBeatsPlayedCard() = false
    final var game = new GameContext(List.of(CUATRO_COPA), 0, 0, null, 0, 2,
        false, true, false, false, POINTS_TO_WIN, 0);
    final var view = new BotMatchView(game, new TrucoContext(TRUCO_CALL, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
    assertThat(new LockAndMazoRule().apply(ctx(view))).isEmpty();
  }

  @Test
  void no_opera_si_esta_respondiendo_al_rival() {

    // Bot debe responder (mustRespond=true): LockAndMazoRule no actúa durante respuesta
    final var game = new GameContext(List.of(CUATRO_COPA), 0, 0, ANCHO_ESPADA, 0, 2,
        false, true, false, false, POINTS_TO_WIN, 0);
    final var view = new BotMatchView(game,
        new TrucoContext(TRUCO_CALL,
            List.of(com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse.QUIERO,
                com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse.NO_QUIERO),
            TRUCO_CALL),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
    // Cuando mustRespond, la regla deja que ResponseToRivalCallRule o el fallback decidan
    assertThat(new LockAndMazoRule().apply(ctx(view))).isEmpty();
  }

  @Test
  void prioridad_es_40() {

    assertThat(new LockAndMazoRule().priority()).isEqualTo(40);
  }

}
