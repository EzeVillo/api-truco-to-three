package com.villo.truco.domain.model.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class HandStrengthEvaluatorTest {

  private static final BotCard ANCHO_ESPADA = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotCard TRES_COPA = new BotCard(10, Card.of(Suit.COPA, 3));
  private static final BotCard CUATRO_COPA = new BotCard(1, Card.of(Suit.COPA, 4));

  private static Offset<Double> within(final double precision) {

    return Offset.offset(precision);
  }

  @Test
  void contextualStrength_emptyCards_returnsZero() {

    assertThat(HandStrengthEvaluator.contextualStrength(List.of(), null, 0)).isEqualTo(0.0);
  }

  @Test
  void contextualStrength_nullCards_returnsZero() {

    assertThat(HandStrengthEvaluator.contextualStrength(null, null, 0)).isEqualTo(0.0);
  }

  @Test
  void contextualStrength_singleBestCard_returnsOne() {

    final var strength = HandStrengthEvaluator.contextualStrength(List.of(ANCHO_ESPADA), null, 0);
    assertThat(strength).isEqualTo(1.0);
  }

  @Test
  void contextualStrength_weakCard_returnsLowValue() {

    final var strength = HandStrengthEvaluator.contextualStrength(List.of(CUATRO_COPA), null, 0);
    assertThat(strength).isLessThan(0.2);
  }

  @Test
  void contextualStrength_withRivalCard_canBeat_noReduction() {

    final var withRival = HandStrengthEvaluator.contextualStrength(List.of(ANCHO_ESPADA), TRES_COPA,
        0);
    final var withoutRival = HandStrengthEvaluator.contextualStrength(List.of(ANCHO_ESPADA), null,
        0);
    assertThat(withRival).isEqualTo(withoutRival);
  }

  @Test
  void contextualStrength_withRivalCard_cannotBeat_reduced() {

    final var withRival = HandStrengthEvaluator.contextualStrength(List.of(CUATRO_COPA), TRES_COPA,
        0);
    final var withoutRival = HandStrengthEvaluator.contextualStrength(List.of(CUATRO_COPA), null,
        0);
    assertThat(withRival).isEqualTo(withoutRival * 0.6, within(0.001));
  }

  @Test
  void contextualStrength_twoCards_strongerWeightedMore() {

    final var strength = HandStrengthEvaluator.contextualStrength(List.of(CUATRO_COPA, TRES_COPA),
        null, 0);
    assertThat(strength).isCloseTo(0.5, within(0.001));
  }

  @Test
  void trucoStrength_bestCard_returnsOne() {

    assertThat(HandStrengthEvaluator.trucoStrength(List.of(ANCHO_ESPADA))).isEqualTo(1.0);
  }

  @Test
  void trucoStrength_empty_returnsZero() {

    assertThat(HandStrengthEvaluator.trucoStrength(List.of())).isEqualTo(0.0);
  }

}
