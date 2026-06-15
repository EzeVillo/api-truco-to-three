package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula, enumerando el mazo en tiempo real, la probabilidad de que el envido del bot le gane el
 * tanto a una mano rival posible. Las cartas que el bot tiene en mano (y, si ya jugó, la carta que
 * el rival mostró) se descuentan del mazo: son cartas que el rival no puede tener, lo que afina la
 * estimación. Si el rival ya jugó una carta, la mano rival se condiciona a contenerla.
 */
final class EnvidoProbabilityCalculator {

  private static final int[] CARD_NUMBERS = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12};

  private EnvidoProbabilityCalculator() {

  }

  static double probabilityBotWinsTanto(final List<BotCard> myCards, final int myEnvido,
      final boolean isMano, final BotCard rivalCardPlayed) {

    final var known = new ArrayList<Card>();
    for (final var botCard : myCards) {
      known.add(botCard.card());
    }
    final Card shownCard = rivalCardPlayed == null ? null : rivalCardPlayed.card();
    if (shownCard != null) {
      known.add(shownCard);
    }

    final var deck = new ArrayList<Card>();
    for (final var suit : Suit.values()) {
      for (final var number : CARD_NUMBERS) {
        final var card = Card.of(suit, number);
        if (!known.contains(card)) {
          deck.add(card);
        }
      }
    }

    long favorable = 0;
    long total = 0;
    final int deckSize = deck.size();

    if (shownCard == null) {
      for (int i = 0; i < deckSize - 2; i++) {
        for (int j = i + 1; j < deckSize - 1; j++) {
          for (int k = j + 1; k < deckSize; k++) {
            final int rivalEnvido = envido(List.of(deck.get(i), deck.get(j), deck.get(k)));
            total++;
            if (botWinsTanto(myEnvido, rivalEnvido, isMano)) {
              favorable++;
            }
          }
        }
      }
    } else {
      for (int i = 0; i < deckSize - 1; i++) {
        for (int j = i + 1; j < deckSize; j++) {
          final int rivalEnvido = envido(List.of(shownCard, deck.get(i), deck.get(j)));
          total++;
          if (botWinsTanto(myEnvido, rivalEnvido, isMano)) {
            favorable++;
          }
        }
      }
    }

    if (total == 0) {
      return 0.0;
    }
    return (double) favorable / total;
  }

  private static boolean botWinsTanto(final int myEnvido, final int rivalEnvido,
      final boolean isMano) {

    if (myEnvido > rivalEnvido) {
      return true;
    }
    return isMano && myEnvido == rivalEnvido;
  }

  private static int envido(final List<Card> cards) {

    var best = 0;
    for (final var suit : Suit.values()) {
      final var valuesOfSuit = new ArrayList<Integer>();
      for (final var card : cards) {
        if (card.suit() == suit) {
          valuesOfSuit.add(envidoValue(card));
        }
      }
      if (valuesOfSuit.size() >= 2) {
        valuesOfSuit.sort(null);
        final int pair = valuesOfSuit.get(valuesOfSuit.size() - 1)
            + valuesOfSuit.get(valuesOfSuit.size() - 2) + 20;
        best = Math.max(best, pair);
      }
    }
    for (final var card : cards) {
      best = Math.max(best, envidoValue(card));
    }
    return best;
  }

  private static int envidoValue(final Card card) {

    final int number = card.number();
    if (number >= 10) {
      return 0;
    }
    return number;
  }

}
