package com.villo.truco.domain.model.bot.decision.rules;

import com.villo.truco.domain.model.bot.decision.CardLockAnalyzer;
import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.DecisionRule;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class LockAndMazoRule implements DecisionRule {

  private static final int PRIORITY = 40;

  @Override
  public Optional<BotAction> apply(final DecisionContext ctx) {

    final var truco = ctx.view().truco();
    if (truco.mustRespond()) {
      return Optional.empty();
    }

    final var game = ctx.view().game();
    final var lock = ctx.lock();

    if (lock.leadsToLockIfAdvance()) {
      return Optional.of(new BotAction.PlayCard(bestBeatingCard(game.myCards(), game.rivalCardPlayed())));
    }

    if (lock.rivalIsOutOfCards() && game.rivalCardPlayed() != null) {
      if (lock.botBeatsPlayedCard()) {
        // rivalCardsInHand=0 and bot can beat → leadsToLockIfAdvance was false only because myCards.size()==1
        return Optional.of(new BotAction.PlayCard(bestBeatingCard(game.myCards(), game.rivalCardPlayed())));
      }
      // Bot can't beat rival's card, rival can't QYMVAM → call truco or fold after truco accepted
      if (game.canFold()) {
        return Optional.of(new BotAction.Fold());
      }
      if (truco.canCall()) {
        return Optional.of(new BotAction.CallTruco(truco.availableCall()));
      }
    }

    return Optional.empty();
  }

  @Override
  public int priority() {

    return PRIORITY;
  }

  @Override
  public String name() {

    return "LockAndMazoRule";
  }

  private static BotCard bestBeatingCard(final List<BotCard> myCards, final BotCard rivalCardPlayed) {

    final int rivalRank = rivalCardPlayed.trucoRank();
    return myCards.stream()
        .filter(c -> c.trucoRank() > rivalRank)
        .min(Comparator.comparingInt(BotCard::trucoRank))
        .orElseGet(() -> myCards.stream().min(Comparator.comparingInt(BotCard::trucoRank)).orElseThrow());
  }

}
