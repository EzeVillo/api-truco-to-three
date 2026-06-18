package com.villo.truco.domain.model.bot.decision;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.ArrayList;
import java.util.List;

public final class UnplayedHandProbability {

  private static final int[] CARD_NUMBERS = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12};

  private final double probability;

  public UnplayedHandProbability(final List<BotCard> myCards, final BotCard rivalCardPlayed) {

    this.probability = compute(myCards, rivalCardPlayed);
  }

  public double probabilityHighCardWinsUnplayedTrick() {

    return probability;
  }

  private static double compute(final List<BotCard> myCards, final BotCard rivalCardPlayed) {

    if (myCards == null || myCards.isEmpty()) {
      return 0.0;
    }

    final int myBestRank = myCards.stream().mapToInt(BotCard::trucoRank).max().orElse(0);

    final var known = new ArrayList<Card>();
    for (final var card : myCards) {
      known.add(card.card());
    }
    if (rivalCardPlayed != null) {
      known.add(rivalCardPlayed.card());
    }

    final var remainingDeck = new ArrayList<Card>();
    for (final var suit : Suit.values()) {
      for (final var number : CARD_NUMBERS) {
        final var card = Card.of(suit, number);
        if (!known.contains(card)) {
          remainingDeck.add(card);
        }
      }
    }

    if (remainingDeck.isEmpty()) {
      return 0.0;
    }

    long favorable = 0;
    for (final var rivalPossibleCard : remainingDeck) {
      if (myBestRank > trucoRank(rivalPossibleCard)) {
        favorable++;
      }
    }

    return (double) favorable / remainingDeck.size();
  }

  private static int trucoRank(final Card card) {

    final int n = card.number();
    final var s = card.suit();
    if (n == 1 && s == Suit.ESPADA) return 14;
    if (n == 1 && s == Suit.BASTO) return 13;
    if (n == 7 && s == Suit.ESPADA) return 12;
    if (n == 7 && s == Suit.ORO) return 11;
    return switch (n) {
      case 3 -> 10;
      case 2 -> 9;
      case 1 -> 8;
      case 12 -> 7;
      case 11 -> 6;
      case 10 -> 5;
      case 7 -> 4;
      case 6 -> 3;
      case 5 -> 2;
      case 4 -> 1;
      default -> throw new IllegalStateException("Unexpected card number: " + n);
    };
  }

}
