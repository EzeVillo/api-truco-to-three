package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import com.villo.truco.domain.model.match.MatchPlayerDecisionView;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchToBotACLTest {

  @Test
  @DisplayName("traduce la vista de dominio al lenguaje del bot sin recalcular negocio")
  void translatesDecisionViewIntoBotView() {

    final var myCard = Card.of(Suit.ESPADA, 1);
    final var rivalCard = Card.of(Suit.BASTO, 7);
    final var view = new MatchPlayerDecisionView(new MatchPlayerDecisionView.GameContext(
        List.of(new MatchPlayerDecisionView.CardView(14, myCard)), 1, 2,
        new MatchPlayerDecisionView.CardView(4, rivalCard), 27, 1, true, true, false, 5),
        new MatchPlayerDecisionView.TrucoContext(TrucoCall.RETRUCO,
            List.of(TrucoResponse.QUIERO, TrucoResponse.NO_QUIERO), TrucoCall.TRUCO),
        new MatchPlayerDecisionView.EnvidoContext(
            List.of(new MatchPlayerDecisionView.EnvidoOption(EnvidoCall.FALTA_ENVIDO, 2, 1)),
            List.of(EnvidoResponse.QUIERO),
            List.of(new MatchPlayerDecisionView.EnvidoOption(EnvidoCall.ENVIDO, 2, 2)),
            new MatchPlayerDecisionView.PendingEnvidoOutcome(2, 2, 1)));

    final var botView = MatchToBotACL.translate(view);

    assertThat(botView.game().myCards()).singleElement().satisfies(card -> {
      assertThat(card.trucoRank()).isEqualTo(14);
      assertThat(card.card()).isEqualTo(myCard);
    });
    assertThat(botView.game().rivalCardPlayed().trucoRank()).isEqualTo(4);
    assertThat(botView.game().myScore()).isEqualTo(1);
    assertThat(botView.game().rivalScore()).isEqualTo(2);
    assertThat(botView.truco().availableCall()).satisfies(call -> {
      assertThat(call.stakeIfAccepted()).isEqualTo(3);
      assertThat(call.stakeIfRejected()).isEqualTo(2);
    });
    assertThat(botView.truco().currentCall().stakeIfAccepted()).isEqualTo(2);
    assertThat(botView.truco().currentCall().stakeIfRejected()).isEqualTo(1);
    assertThat(botView.envido().availableCalls()).singleElement().satisfies(call -> {
      assertThat(call.level()).isEqualTo(BotEnvidoLevel.FALTA_ENVIDO);
      assertThat(call.acceptedPointsIfBotWins()).isEqualTo(2);
      assertThat(call.acceptedPointsIfRivalWins()).isEqualTo(1);
      assertThat(call.rejectedPointsIfRivalDeclines()).isEqualTo(2);
    });
    assertThat(botView.envido().pendingOutcome()).satisfies(outcome -> {
      assertThat(outcome.acceptedPointsIfBotWins()).isEqualTo(2);
      assertThat(outcome.acceptedPointsIfRivalWins()).isEqualTo(2);
      assertThat(outcome.rejectedPoints()).isEqualTo(1);
    });
  }

  @Test
  @DisplayName("mantiene el mapeo inverso desde el dominio bot a match")
  void mapsBotActionsBackToMatchTypes() {

    final var botCard = MatchToBotACL.translate(new MatchPlayerDecisionView(
            new MatchPlayerDecisionView.GameContext(
                List.of(new MatchPlayerDecisionView.CardView(6, Card.of(Suit.ORO, 10))), 0, 0, null, 0,
                0, false, true, false, 5),
            new MatchPlayerDecisionView.TrucoContext(null, List.of(), null),
            new MatchPlayerDecisionView.EnvidoContext(List.of(), List.of(), List.of(), null))).game()
        .myCards().getFirst();

    assertThat(MatchToBotACL.toCard(botCard)).isEqualTo(Card.of(Suit.ORO, 10));
    assertThat(MatchToBotACL.toTrucoResponse(BotTrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO)).isEqualTo(
        TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO);
    assertThat(
        MatchToBotACL.toEnvidoCall(new BotEnvidoCall(2, 1, 1, BotEnvidoLevel.REAL_ENVIDO))).isEqualTo(
        EnvidoCall.REAL_ENVIDO);
    assertThat(MatchToBotACL.toEnvidoResponse(BotEnvidoResponse.NO_QUIERO)).isEqualTo(
        EnvidoResponse.NO_QUIERO);
  }

  @Test
  @DisplayName("proyecta los totales acumulados del envido al traducir opciones de subida")
  void projectsAccumulatedEnvidoTotalsForAvailableCalls() {

    final var view = new MatchPlayerDecisionView(
        new MatchPlayerDecisionView.GameContext(List.of(), 1, 1, null, 30, 0, false, true, false,
            3),
        new MatchPlayerDecisionView.TrucoContext(null, List.of(), null),
        new MatchPlayerDecisionView.EnvidoContext(
            List.of(new MatchPlayerDecisionView.EnvidoOption(EnvidoCall.REAL_ENVIDO, 3, 3)),
            List.of(EnvidoResponse.QUIERO, EnvidoResponse.NO_QUIERO),
            List.of(new MatchPlayerDecisionView.EnvidoOption(EnvidoCall.ENVIDO, 2, 2)),
            new MatchPlayerDecisionView.PendingEnvidoOutcome(2, 2, 1)));

    final var botView = MatchToBotACL.translate(view);

    assertThat(botView.envido().availableCalls()).singleElement().satisfies(call -> {
      assertThat(call.level()).isEqualTo(BotEnvidoLevel.REAL_ENVIDO);
      assertThat(call.acceptedPointsIfBotWins()).isEqualTo(5);
      assertThat(call.acceptedPointsIfRivalWins()).isEqualTo(5);
      assertThat(call.rejectedPointsIfRivalDeclines()).isEqualTo(2);
    });
  }
}
