package com.villo.truco.domain.model.match;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import java.util.List;
import java.util.stream.Collectors;

final class EnvidoCalculator {

  private EnvidoCalculator() {

  }

  static int calculate(final List<Card> cards) {

    final var cardsBySuit = cards.stream().collect(Collectors.groupingBy(Card::suit));

    return cardsBySuit.values().stream().filter(cardsOfSuit -> cardsOfSuit.size() >= 2).mapToInt(
            cardsOfSuit -> cardsOfSuit.stream().mapToInt(EnvidoCalculator::envidoValue).sorted()
                .skip(cardsOfSuit.size() - 2).sum() + 20).max()
        .orElseGet(() -> cards.stream().mapToInt(EnvidoCalculator::envidoValue).max().orElse(0));
  }

  private static int envidoValue(final Card card) {

    final int number = card.number();
    if (number >= 10) {
      return 0;
    }
    return number;
  }

}

