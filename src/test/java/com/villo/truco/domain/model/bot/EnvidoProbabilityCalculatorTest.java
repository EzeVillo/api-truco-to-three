package com.villo.truco.domain.model.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class EnvidoProbabilityCalculatorTest {

  private static BotCard card(final Suit suit, final int number) {

    return new BotCard(0, Card.of(suit, number));
  }

  @Test
  void maxEnvidoAsMano_winsAlmostAlways() {

    // 33 (6 y 7 de espada) como mano gana el tanto siempre: nadie lo supera y los empates son suyos.
    final var myCards = List.of(card(Suit.ESPADA, 6), card(Suit.ESPADA, 7), card(Suit.BASTO, 3));

    final var probability = EnvidoProbabilityCalculator.probabilityBotWinsTanto(myCards, 33, true,
        null);

    assertThat(probability).isEqualTo(1.0);
  }

  @Test
  void lowEnvidoAsMano_isUnderdog() {

    // 7 (cartas de distinto palo) como mano: por debajo del 50%.
    final var myCards = List.of(card(Suit.ESPADA, 7), card(Suit.BASTO, 6), card(Suit.ORO, 5));

    final var probability = EnvidoProbabilityCalculator.probabilityBotWinsTanto(myCards, 7, true,
        null);

    assertThat(probability).isLessThan(0.5).isCloseTo(0.407, Offset.offset(0.02));
  }

  @Test
  void envido22AsMano_isNearCoinFlip() {

    // El cruce del 50% vive en ~22: confirma el umbral con el que se elige falta vs envido.
    final var myCards = List.of(card(Suit.ESPADA, 2), card(Suit.ESPADA, 10), card(Suit.BASTO, 3));

    final var probability = EnvidoProbabilityCalculator.probabilityBotWinsTanto(myCards, 22, true,
        null);

    assertThat(probability).isCloseTo(0.5, Offset.offset(0.04));
  }

  @Test
  void rivalShownCardConditionsTheEstimate() {

    // Misma mano (22) como pie: si el rival mostró un 7 (tanto alto) el bot deja de ser favorito;
    // si mostró una figura (tanto bajo) pasa a serlo. La carta jugada cambia la decisión.
    final var myCards = List.of(card(Suit.ESPADA, 2), card(Suit.ESPADA, 10), card(Suit.BASTO, 3));

    final var withHighTantoCard = EnvidoProbabilityCalculator.probabilityBotWinsTanto(myCards, 22,
        false, card(Suit.ORO, 7));
    final var withLowTantoCard = EnvidoProbabilityCalculator.probabilityBotWinsTanto(myCards, 22,
        false, card(Suit.ORO, 12));

    assertThat(withHighTantoCard).isLessThan(0.5);
    assertThat(withLowTantoCard).isGreaterThan(0.5);
    assertThat(withHighTantoCard).isLessThan(withLowTantoCard);
  }

}
