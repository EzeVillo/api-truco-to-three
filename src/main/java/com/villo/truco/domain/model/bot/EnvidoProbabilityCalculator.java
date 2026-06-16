package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.ArrayList;
import java.util.List;

final class EnvidoProbabilityCalculator {

  private static final int[] CARD_NUMBERS = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12};

  private EnvidoProbabilityCalculator() {

  }

  static double probabilityBotWinsTanto(final EnvidoScoring scoring, final List<BotCard> myCards,
      final int myEnvido, final boolean isMano, final BotCard rivalCardPlayed) {

    final var known = new ArrayList<Card>();
    for (final var botCard : myCards) {
      known.add(botCard.card());
    }
    final var shownCard = rivalCardPlayed == null ? null : rivalCardPlayed.card();
    if (shownCard != null) {
      if (myEnvido < scoring.of(List.of(shownCard))) {
        return 0.0;
      }
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
    final var deckSize = deck.size();

    if (shownCard == null) {
      for (var i = 0; i < deckSize - 2; i++) {
        for (var j = i + 1; j < deckSize - 1; j++) {
          for (var k = j + 1; k < deckSize; k++) {
            final var rivalEnvido = scoring.of(List.of(deck.get(i), deck.get(j), deck.get(k)));
            total++;
            if (botWinsTanto(myEnvido, rivalEnvido, isMano)) {
              favorable++;
            }
          }
        }
      }
    } else {
      for (var i = 0; i < deckSize - 1; i++) {
        for (var j = i + 1; j < deckSize; j++) {
          final var rivalEnvido = scoring.of(List.of(shownCard, deck.get(i), deck.get(j)));
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

}
