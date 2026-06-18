package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.BotDecisionEngine;
import com.villo.truco.domain.model.bot.EnvidoScoring;
import com.villo.truco.domain.model.bot.LegacyBotDecisionEngine;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.MatchPlayerDecisionView;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.function.Function;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Benchmark empírico de SC-007: enfrenta el motor rediseñado ({@link BotDecisionEngine}) contra el
 * motor anterior ({@link LegacyBotDecisionEngine}) en partidas a 3 puntos y mide el win-rate.
 *
 * <p>Reutiliza el camino bot-vs-bot a nivel de dominio: crea un {@link Match}, y por cada turno
 * resuelve qué bot actúa ({@link MatchPlayerDecisionView#hasAvailableActions()}), traduce la vista
 * con {@link MatchToBotACL} y despacha la {@link BotAction} al match — igual que
 * {@code ExecuteBotTurnCommandHandler} + {@code AdvanceBotVsBotMatchCommandHandler}.
 *
 * <p><b>@Disabled</b>: el reparto de cartas ({@code Deck.create()} usa {@code Collections.shuffle}
 * sin seed) y los motores usan {@code new Random()}, así que el resultado no es determinista. Se
 * ejecuta manualmente para validar el win-rate y documentar la muestra en
 * {@code specs/017-bot-decision-engine/research.md}. Habilitar temporalmente para correrlo.
 */
@DisplayName("SC-007 — benchmark win-rate motor rediseñado vs. anterior")
@Disabled("Benchmark empírico no determinista; ejecutar manualmente para validar SC-007")
class BotEngineHeadToHeadBenchmarkTest {

  private static final int SAMPLE = 120;
  private static final int MAX_ACTIONS_PER_MATCH = 1000;
  private static final BotPersonality PERSONALITY = new BotPersonality(50, 50, 50, 50, 50);
  private static final EnvidoScoring SCORING = new MatchEnvidoScoring();
  private static final MatchRules SINGLE_GAME_TO_THREE = new MatchRules(1, false);

  private static PlayerId playOneMatch(final PlayerId playerOne, final PlayerId playerTwo,
      final boolean newEngineIsPlayerOne, final Function<Match, BotDecisionEngine> newEngineFactory,
      final Function<Match, LegacyBotDecisionEngine> legacyEngineFactory) {

    final var match = Match.createReady(playerOne, playerTwo, SINGLE_GAME_TO_THREE);
    match.startMatch(playerOne);
    match.startMatch(playerTwo);

    final var newEngine = newEngineFactory.apply(match);
    final var legacyEngine = legacyEngineFactory.apply(match);

    int actions = 0;
    while (!match.isFinished() && actions < MAX_ACTIONS_PER_MATCH) {
      final PlayerId actor = resolveActor(match, playerOne, playerTwo);
      if (actor == null) {
        break;
      }
      final var view = MatchToBotACL.translate(match.getDecisionViewFor(actor));
      final BotAction action =
          actor.equals(playerOne) == newEngineIsPlayerOne ? newEngine.decide(view)
              : legacyEngine.decide(view);
      dispatch(match, actor, action);
      actions++;
    }

    if (!match.isFinished()) {
      throw new IllegalStateException(
          "Partida no terminó tras " + MAX_ACTIONS_PER_MATCH + " acciones");
    }
    final var winner = match.getMatchWinner();
    if (winner == null) {
      throw new IllegalStateException("Partida finalizada sin ganador");
    }
    return winner;
  }

  private static PlayerId resolveActor(final Match match, final PlayerId playerOne,
      final PlayerId playerTwo) {

    if (match.getDecisionViewFor(playerOne).hasAvailableActions()) {
      return playerOne;
    }
    if (match.getDecisionViewFor(playerTwo).hasAvailableActions()) {
      return playerTwo;
    }
    return null;
  }

  private static void dispatch(final Match match, final PlayerId actor, final BotAction action) {

    switch (action) {
      case BotAction.PlayCard pc -> match.playCard(actor, MatchToBotACL.toCard(pc.card()));
      case BotAction.CallTruco ignored -> match.callTruco(actor);
      case BotAction.RespondTruco rt -> {
        switch (rt.response()) {
          case QUIERO -> match.acceptTruco(actor);
          case NO_QUIERO -> match.rejectTruco(actor);
          case QUIERO_Y_ME_VOY_AL_MAZO -> match.acceptTrucoAndFold(actor);
        }
      }
      case BotAction.CallEnvido ce ->
          match.callEnvido(actor, MatchToBotACL.toEnvidoCall(ce.call()));
      case BotAction.RespondEnvido re -> {
        if (re.response() == BotEnvidoResponse.QUIERO) {
          match.acceptEnvido(actor);
        } else {
          match.rejectEnvido(actor);
        }
      }
      case BotAction.Fold ignored -> match.fold(actor);
    }
  }

  @Test
  @DisplayName("el motor rediseñado gana ≥ 60% de las partidas contra el anterior")
  void redesignedEngineBeatsLegacyEngineByAtLeastSixtyPercent() {

    final Function<Match, BotDecisionEngine> newEngineFactory = m -> new BotDecisionEngine(
        PERSONALITY, SCORING);
    final Function<Match, LegacyBotDecisionEngine> legacyEngineFactory = m -> new LegacyBotDecisionEngine(
        PERSONALITY, SCORING);

    int newEngineWins = 0;
    int completed = 0;
    int errors = 0;

    for (int i = 0; i < SAMPLE; i++) {
      final boolean newEngineIsPlayerOne = i % 2 == 0;
      final var playerOne = PlayerId.generate();
      final var playerTwo = PlayerId.generate();

      try {
        final PlayerId winner = playOneMatch(playerOne, playerTwo, newEngineIsPlayerOne,
            newEngineFactory, legacyEngineFactory);
        completed++;
        final boolean newEngineWon =
            newEngineIsPlayerOne ? winner.equals(playerOne) : winner.equals(playerTwo);
        if (newEngineWon) {
          newEngineWins++;
        }
      } catch (final RuntimeException ex) {
        errors++;
      }
    }

    final double winRate = completed == 0 ? 0.0 : (double) newEngineWins / completed;
    System.out.printf(
        "[SC-007] partidas=%d completadas=%d victoriasNueva=%d errores=%d winRate=%.3f%n", SAMPLE,
        completed, newEngineWins, errors, winRate);

    assertThat(completed).as("el benchmark debió completar partidas").isGreaterThan(0);
    assertThat(winRate).as("win-rate del motor rediseñado vs. el anterior (SC-007 ≥ 0.60)")
        .isGreaterThanOrEqualTo(0.60);
  }

}
