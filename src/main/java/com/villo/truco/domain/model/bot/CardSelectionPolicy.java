package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.exceptions.BotWithoutCardsException;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class CardSelectionPolicy {

  private final BotPersonality personality;
  private final Random random;

  CardSelectionPolicy(final BotPersonality personality, final Random random) {

    this.personality = personality;
    this.random = random;
  }

  BotCard select(final List<BotCard> myCards, final BotCard rivalCardPlayed) {

    if (myCards.isEmpty()) {
      throw new BotWithoutCardsException();
    }

    final var sorted = myCards.stream().sorted(Comparator.comparingInt(BotCard::trucoRank))
        .toList();

    if (rivalCardPlayed != null) {
      return selectWhenRivalPlayed(sorted, rivalCardPlayed);
    }
    return this.selectWhenLeading(sorted);
  }

  private static BotCard selectWhenRivalPlayed(final List<BotCard> sortedAscending,
      final BotCard rivalCard) {

    final int rivalValue = rivalCard.trucoRank();
    return sortedAscending.stream().filter(c -> c.trucoRank() > rivalValue).findFirst()
        .orElse(sortedAscending.getFirst());
  }

  private BotCard selectWhenLeading(final List<BotCard> sortedAscending) {

    if (this.personality.aguantador() >= 60) {
      return sortedAscending.getFirst();
    }

    if (this.personality.temerario() >= 65) {
      return sortedAscending.getLast();
    }

    return sortedAscending.get(this.random.nextInt(sortedAscending.size()));
  }

}
