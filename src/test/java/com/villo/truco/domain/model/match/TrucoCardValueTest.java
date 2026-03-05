package com.villo.truco.domain.model.match;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.Suit;
import org.junit.jupiter.api.Test;

class TrucoCardValueTest {

  @Test
  void anchoDeEspadaShouldBeTheHighestCard() {

    assertThat(TrucoCardValue.of(Card.of(Suit.ESPADA, 1))).isEqualTo(14);
  }

  @Test
  void anchoDeBastoShouldBeSecondHighest() {

    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 1))).isEqualTo(13);
  }

  @Test
  void sieteDeEspadaShouldBeThirdHighest() {

    assertThat(TrucoCardValue.of(Card.of(Suit.ESPADA, 7))).isEqualTo(12);
  }

  @Test
  void sieteDeOroShouldBeFourthHighest() {

    assertThat(TrucoCardValue.of(Card.of(Suit.ORO, 7))).isEqualTo(11);
  }

  @Test
  void tresShouldHaveSameValueRegardlessSuit() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 3))).isEqualTo(10);
    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 3))).isEqualTo(10);
    assertThat(TrucoCardValue.of(Card.of(Suit.ORO, 3))).isEqualTo(10);
    assertThat(TrucoCardValue.of(Card.of(Suit.ESPADA, 3))).isEqualTo(10);
  }

  @Test
  void dosShouldHaveSameValueRegardlessSuit() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 2))).isEqualTo(9);
    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 2))).isEqualTo(9);
    assertThat(TrucoCardValue.of(Card.of(Suit.ORO, 2))).isEqualTo(9);
    assertThat(TrucoCardValue.of(Card.of(Suit.ESPADA, 2))).isEqualTo(9);
  }

  @Test
  void unoDeCopaAndOroShouldHaveSameValue() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 1))).isEqualTo(8);
    assertThat(TrucoCardValue.of(Card.of(Suit.ORO, 1))).isEqualTo(8);
  }

  @Test
  void doceShouldHaveSameValueRegardlessSuit() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 12))).isEqualTo(7);
    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 12))).isEqualTo(7);
    assertThat(TrucoCardValue.of(Card.of(Suit.ORO, 12))).isEqualTo(7);
    assertThat(TrucoCardValue.of(Card.of(Suit.ESPADA, 12))).isEqualTo(7);
  }

  @Test
  void onceShouldHaveSameValueRegardlessSuit() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 11))).isEqualTo(6);
    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 11))).isEqualTo(6);
  }

  @Test
  void diezShouldHaveSameValueRegardlessSuit() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 10))).isEqualTo(5);
    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 10))).isEqualTo(5);
  }

  @Test
  void sieteDeCopaAndBastoShouldHaveSameValue() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 7))).isEqualTo(4);
    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 7))).isEqualTo(4);
  }

  @Test
  void seisShouldHaveSameValueRegardlessSuit() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 6))).isEqualTo(3);
    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 6))).isEqualTo(3);
  }

  @Test
  void cincoShouldHaveSameValueRegardlessSuit() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 5))).isEqualTo(2);
    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 5))).isEqualTo(2);
  }

  @Test
  void cuatroShouldBeTheLowestCard() {

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 4))).isEqualTo(1);
    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 4))).isEqualTo(1);
  }

  // ===== JERARQUÍA COMPLETA =====

  @Test
  void shouldRespectFullHierarchy() {

    assertThat(TrucoCardValue.of(Card.of(Suit.ESPADA, 1))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.BASTO, 1)));

    assertThat(TrucoCardValue.of(Card.of(Suit.BASTO, 1))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.ESPADA, 7)));

    assertThat(TrucoCardValue.of(Card.of(Suit.ESPADA, 7))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.ORO, 7)));

    assertThat(TrucoCardValue.of(Card.of(Suit.ORO, 7))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 3)));

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 3))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 2)));

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 2))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 1)));

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 1))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 12)));

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 12))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 11)));

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 11))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 10)));

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 10))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 7)));

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 7))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 6)));

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 6))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 5)));

    assertThat(TrucoCardValue.of(Card.of(Suit.COPA, 5))).isGreaterThan(
        TrucoCardValue.of(Card.of(Suit.COPA, 4)));
  }

}