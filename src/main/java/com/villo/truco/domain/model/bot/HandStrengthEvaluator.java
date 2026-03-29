package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import java.util.List;
import java.util.stream.IntStream;

final class HandStrengthEvaluator {

  private static final double MAX_TRUCO_VALUE = 14.0;

  private HandStrengthEvaluator() {

  }

  static double contextualStrength(final List<BotCard> cards, final BotCard rivalCardPlayed,
      final int handsPlayedCount) {

    if (cards == null || cards.isEmpty()) {
      return 0.0;
    }

    final var values = cards.stream().mapToInt(BotCard::trucoRank).sorted().toArray();

    var weightedSum = 0D;
    var totalWeight = 0D;
    for (int i = 0; i < values.length; i++) {
      final var weight = i + 1D;
      weightedSum += values[i] * weight;
      totalWeight += weight;
    }
    var strength = weightedSum / (totalWeight * MAX_TRUCO_VALUE);

    if (rivalCardPlayed != null) {
      final var rivalValue = rivalCardPlayed.trucoRank();
      final var canBeat = IntStream.of(values).anyMatch(value -> value > rivalValue);
      if (!canBeat) {
        strength *= 0.6;
      }
    }

    return strength;
  }

  static double trucoStrength(final List<BotCard> cards) {

    if (cards == null || cards.isEmpty()) {
      return 0.0;
    }
    final int best = cards.stream().mapToInt(BotCard::trucoRank).max().orElse(0);
    return best / MAX_TRUCO_VALUE;
  }

}
