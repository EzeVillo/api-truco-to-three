package com.villo.truco.domain.model.match;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import org.junit.jupiter.api.Test;

class TrucoCardValueTest {

  @Test
  void anchoDeEspadaShouldBeTheHighestCard() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ESPADA, 1))).isEqualTo(14);
  }

  @Test
  void anchoDeBastoShouldBeSecondHighest() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 1))).isEqualTo(13);
  }

  @Test
  void sieteDeEspadaShouldBeThirdHighest() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ESPADA, 7))).isEqualTo(12);
  }

  @Test
  void sieteDeOroShouldBeFourthHighest() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ORO, 7))).isEqualTo(11);
  }

  @Test
  void tresShouldHaveSameValueRegardlessSuit() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 3))).isEqualTo(10);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 3))).isEqualTo(10);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ORO, 3))).isEqualTo(10);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ESPADA, 3))).isEqualTo(10);
  }

  @Test
  void dosShouldHaveSameValueRegardlessSuit() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 2))).isEqualTo(9);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 2))).isEqualTo(9);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ORO, 2))).isEqualTo(9);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ESPADA, 2))).isEqualTo(9);
  }

  @Test
  void unoDeCopaAndOroShouldHaveSameValue() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 1))).isEqualTo(8);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ORO, 1))).isEqualTo(8);
  }

  @Test
  void doceShouldHaveSameValueRegardlessSuit() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 12))).isEqualTo(7);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 12))).isEqualTo(7);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ORO, 12))).isEqualTo(7);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ESPADA, 12))).isEqualTo(7);
  }

  @Test
  void onceShouldHaveSameValueRegardlessSuit() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 11))).isEqualTo(6);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 11))).isEqualTo(6);
  }

  @Test
  void diezShouldHaveSameValueRegardlessSuit() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 10))).isEqualTo(5);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 10))).isEqualTo(5);
  }

  @Test
  void sieteDeCopaAndBastoShouldHaveSameValue() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 7))).isEqualTo(4);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 7))).isEqualTo(4);
  }

  @Test
  void seisShouldHaveSameValueRegardlessSuit() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 6))).isEqualTo(3);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 6))).isEqualTo(3);
  }

  @Test
  void cincoShouldHaveSameValueRegardlessSuit() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 5))).isEqualTo(2);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 5))).isEqualTo(2);
  }

  @Test
  void cuatroShouldBeTheLowestCard() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 4))).isEqualTo(1);
    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 4))).isEqualTo(1);
  }

  // ===== JERARQUÍA COMPLETA =====

  @Test
  void shouldRespectFullHierarchy() {

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ESPADA, 1))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 1)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.BASTO, 1))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.ESPADA, 7)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ESPADA, 7))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.ORO, 7)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.ORO, 7))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 3)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 3))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 2)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 2))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 1)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 1))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 12)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 12))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 11)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 11))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 10)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 10))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 7)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 7))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 6)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 6))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 5)));

    assertThat(CardEvaluationService.trucoValue(Card.of(Suit.COPA, 5))).isGreaterThan(
        CardEvaluationService.trucoValue(Card.of(Suit.COPA, 4)));
  }

}
