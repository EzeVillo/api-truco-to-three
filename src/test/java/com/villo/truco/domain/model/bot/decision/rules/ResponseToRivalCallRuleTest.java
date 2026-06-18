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
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponseToRivalCallRuleTest {

  private static final int POINTS_TO_WIN = 3;
  private static final BotCard ANCHO = new BotCard(14, Card.of(Suit.ESPADA, 1));
  // TRUCO_CALL: accepted=2 puntos para quien gana, rejected=1 punto para el rival
  private static final BotTrucoCall TRUCO_CALL = new BotTrucoCall(2, 1);
  // RETRUCO_CALL: accepted=3, rejected=2
  private static final BotTrucoCall RETRUCO_CALL = new BotTrucoCall(3, 2);

  private static DecisionContext ctx(final BotMatchView view) {

    final var game = view.game();
    final var arithmetic = new MatchArithmetic(game.myScore(), game.rivalScore(),
        game.pointsToWin());
    final var lock = new CardLockAnalyzer(game);
    final var tanto = TantoProbabilityProvider.withKnownProbability(0.5);
    final var unplayed = new UnplayedHandProbability(game.myCards(), game.rivalCardPlayed());
    return new DecisionContext(view, arithmetic, lock, tanto, unplayed);
  }

  private static BotMatchView respondeTruco(final List<BotCard> cards, final int myScore,
      final int rivalScore, final BotTrucoCall pendingCall,
      final List<BotTrucoResponse> availableResponses) {

    final var game = new GameContext(cards, myScore, rivalScore, null, 0, 0, false, false, false,
        false, POINTS_TO_WIN, cards.size());
    return new BotMatchView(game, new TrucoContext(null, availableResponses, pendingCall),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
  }

  @Test
  void rivalSePasaAlAceptar_QYMVAM_disponible_devuelveQYMVAM() {

    // rival=2, truco accepted=2 → 2+2=4 > 3 → busts; QYMVAM disponible
    final var view = respondeTruco(List.of(ANCHO), 0, 2, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO,
            BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO));
    final var result = new ResponseToRivalCallRule().apply(ctx(view));
    assertThat(result).isPresent();
    assertThat(((BotAction.RespondTruco) result.get()).response()).isEqualTo(
        BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO);
  }

  @Test
  void rivalSePasaAlRechazar_devuelveNoQuiero() {

    // rival=2, retruco rejected=2 → 2+2=4 > 3 → busts
    final var view = respondeTruco(List.of(ANCHO), 0, 2, RETRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO));
    final var result = new ResponseToRivalCallRule().apply(ctx(view));
    assertThat(result).isPresent();
    assertThat(((BotAction.RespondTruco) result.get()).response()).isEqualTo(
        BotTrucoResponse.NO_QUIERO);
  }

  @Test
  void rivalSePasaAlAceptar_QYMVAM_noDisponible_noOpina() {

    // rival=2, truco accepted=2 → busts, pero QYMVAM no está en las respuestas
    // → la regla no puede forzar QYMVAM, y NO_QUIERO no mata al rival (rejected=1, 2+1=3, no excede)
    final var view = respondeTruco(List.of(), 0, 2, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO));
    final var result = new ResponseToRivalCallRule().apply(ctx(view));
    assertThat(result).isEmpty();
  }

  @Test
  void rivalNoSePasa_noOpina() {

    // rival=0, truco accepted=2 → 0+2=2 < 3 → no busts; rejected=1 → 0+1=1 < 3 → no busts
    final var view = respondeTruco(List.of(ANCHO), 0, 0, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO,
            BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO));
    final var result = new ResponseToRivalCallRule().apply(ctx(view));
    assertThat(result).isEmpty();
  }

  @Test
  void cuandoNoDebeResponder_noOpina() {

    // Sin mustRespond (availableResponses vacío)
    final var game = new GameContext(List.of(ANCHO), 0, 2, null, 0, 0, false, true, false, false,
        POINTS_TO_WIN, 1);
    final var view = new BotMatchView(game, new TrucoContext(TRUCO_CALL, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
    final var result = new ResponseToRivalCallRule().apply(ctx(view));
    assertThat(result).isEmpty();
  }

  @Test
  void nuncaQYMVAMSinCartas_cuandoBotSinCartas_QYMVAM_estaFueraDeRespuestasDisponibles() {

    // bot sin cartas: QYMVAM no aparece en availableResponses aunque rival se pasaría
    // → la regla verifica canRespondWith(QYMVAM) y no puede forzarlo
    final var view = respondeTruco(List.of(), 0, 2, TRUCO_CALL,
        List.of(BotTrucoResponse.QUIERO, BotTrucoResponse.NO_QUIERO));
    // rival=2, truco rejected=1 → 2+1=3 no excede → no es no-quiero forzado
    // rival=2, truco accepted=2 → 4 > 3 → busts por QYMVAM, pero QYMVAM no disponible → noOpina
    final var result = new ResponseToRivalCallRule().apply(ctx(view));
    assertThat(result).isEmpty();
  }

}
