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

class EnvidoAtTwoTwoRuleTest {

  private static final int POINTS_TO_WIN = 3;
  private static final BotCard CUATRO = new BotCard(1, Card.of(Suit.COPA, 4));

  private static final BotEnvidoCall ENVIDO = new BotEnvidoCall(2, 2, 1, BotEnvidoLevel.ENVIDO);
  private static final BotEnvidoCall FALTA = new BotEnvidoCall(1, 2, 1,
      BotEnvidoLevel.FALTA_ENVIDO);

  private static DecisionContext ctx(final BotMatchView view, final double winProbability) {

    final var game = view.game();
    final var arithmetic = new MatchArithmetic(game.myScore(), game.rivalScore(),
        game.pointsToWin());
    final var lock = new CardLockAnalyzer(game);
    final var tanto = TantoProbabilityProvider.withKnownProbability(winProbability);
    final var unplayed = new UnplayedHandProbability(game.myCards(), game.rivalCardPlayed());
    return new DecisionContext(view, arithmetic, lock, tanto, unplayed);
  }

  private static BotMatchView dos_a_dos(final List<BotEnvidoCall> calls) {

    final var game = new GameContext(List.of(CUATRO), 2, 2, null, 0, 0, false, true, false, false,
        POINTS_TO_WIN, 1);
    return new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(calls, List.of(), List.of(), null));
  }

  @Test
  void dosADos_masProbablePerder_llamaEnvido() {

    // bot más probable de perder el tanto → llama envido (trampa: si rival acepta y gana → busts)
    final var view = dos_a_dos(List.of(ENVIDO, FALTA));
    final var result = new EnvidoAtTwoTwoRule().apply(ctx(view, 0.3));
    assertThat(result).isPresent();
    assertThat(((BotAction.CallEnvido) result.get()).call().level()).isEqualTo(
        BotEnvidoLevel.ENVIDO);
  }

  @Test
  void dosADos_masProbableGanar_llamaFaltaEnvido() {

    final var view = dos_a_dos(List.of(ENVIDO, FALTA));
    final var result = new EnvidoAtTwoTwoRule().apply(ctx(view, 0.7));
    assertThat(result).isPresent();
    assertThat(((BotAction.CallEnvido) result.get()).call().level()).isEqualTo(
        BotEnvidoLevel.FALTA_ENVIDO);
  }

  @Test
  void dosADos_empate50_50_llamaFaltaEnvido() {

    // empate → "no es más probable perder" → falta envido (D6)
    final var view = dos_a_dos(List.of(ENVIDO, FALTA));
    final var result = new EnvidoAtTwoTwoRule().apply(ctx(view, 0.5));
    assertThat(result).isPresent();
    assertThat(((BotAction.CallEnvido) result.get()).call().level()).isEqualTo(
        BotEnvidoLevel.FALTA_ENVIDO);
  }

  @Test
  void noEsDosADos_noOpina() {

    final var game = new GameContext(List.of(CUATRO), 1, 2, null, 0, 0, false, true, false, false,
        POINTS_TO_WIN, 1);
    final var view = new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(ENVIDO, FALTA), List.of(), List.of(), null));
    assertThat(new EnvidoAtTwoTwoRule().apply(ctx(view, 0.3))).isEmpty();
  }

  @Test
  void dosADos_sinEnvidoDisponible_noOpina() {

    final var view = dos_a_dos(List.of());
    assertThat(new EnvidoAtTwoTwoRule().apply(ctx(view, 0.3))).isEmpty();
  }

  @Test
  void dosADos_mustRespond_noOpina() {

    // El bot debe responder envido rival (mustRespond=true): la regla no canta sí o sí
    final var game = new GameContext(List.of(CUATRO), 2, 2, null, 0, 0, false, false, false, false,
        POINTS_TO_WIN, 1);
    final var view = new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(ENVIDO),
            List.of(com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse.QUIERO), List.of(),
            new BotMatchView.PendingEnvidoOutcome(2, 2, 1)));
    assertThat(new EnvidoAtTwoTwoRule().apply(ctx(view, 0.3))).isEmpty();
  }

  @Test
  void dosADos_soloEnvidoDisponible_siMasProbableGanar_llamaEnvido() {

    // Solo ENVIDO disponible (FALTA no existe): debe llamar con lo disponible
    final var view = dos_a_dos(List.of(ENVIDO));
    final var result = new EnvidoAtTwoTwoRule().apply(ctx(view, 0.7));
    assertThat(result).isPresent();
    // FALTA no está disponible → llama ENVIDO (el nivel más alto disponible que coincida)
    assertThat(((BotAction.CallEnvido) result.get()).call().level()).isEqualTo(
        BotEnvidoLevel.ENVIDO);
  }

}
