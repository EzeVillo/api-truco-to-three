package com.villo.truco.domain.model.bot.decision;

import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext;
import java.util.List;

public final class CardLockAnalyzer {

  private final List<BotCard> myCards;
  private final BotCard rivalCardPlayed;
  private final int rivalCardsInHand;

  public CardLockAnalyzer(final GameContext game) {

    this.myCards = game.myCards();
    this.rivalCardPlayed = game.rivalCardPlayed();
    this.rivalCardsInHand = game.rivalCardsInHand();
  }

  public boolean rivalIsOutOfCards() {

    return rivalCardsInHand == 0;
  }

  public boolean rivalCannotQYMVAM() {

    return rivalIsOutOfCards();
  }

  /** Existe al menos una carta propia que mata (trucoRank estrictamente mayor) la carta jugada por el rival. */
  public boolean botBeatsPlayedCard() {

    if (rivalCardPlayed == null) {
      return false;
    }
    final int rivalRank = rivalCardPlayed.trucoRank();
    return myCards.stream().anyMatch(c -> c.trucoRank() > rivalRank);
  }

  /** Rival jugó su última carta y bot puede matarla. */
  public boolean botHasGuaranteedTrick() {

    return rivalIsOutOfCards() && botBeatsPlayedCard();
  }

  /**
   * Bot puede matar la carta jugada por el rival, el rival ya no tiene cartas, y el bot tiene más
   * de una carta (al jugar una le quedan cartas: encierro garantizado al avanzar).
   */
  public boolean leadsToLockIfAdvance() {

    return botHasGuaranteedTrick() && myCards.size() > 1;
  }

}
