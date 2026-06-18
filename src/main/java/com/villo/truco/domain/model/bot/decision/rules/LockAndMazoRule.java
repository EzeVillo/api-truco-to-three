package com.villo.truco.domain.model.bot.decision.rules;

import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.DecisionRule;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotTrucoResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class LockAndMazoRule implements DecisionRule {

  private static final int PRIORITY = 40;
  // Umbral para aceptar un truco del rival apostando a ganar la mano no jugada
  static final double ACCEPT_THRESHOLD = 0.60;

  @Override
  public Optional<BotAction> apply(final DecisionContext ctx) {

    final var truco = ctx.view().truco();
    final var game = ctx.view().game();

    if (truco.mustRespond()) {
      return handleMustRespond(truco, ctx);
    }

    final var lock = ctx.lock();

    if (lock.leadsToLockIfAdvance()) {
      return Optional.of(
          new BotAction.PlayCard(bestBeatingCard(game.myCards(), game.rivalCardPlayed())));
    }

    if (lock.rivalIsOutOfCards() && game.rivalCardPlayed() != null) {
      if (lock.botBeatsPlayedCard()) {
        return Optional.of(
            new BotAction.PlayCard(bestBeatingCard(game.myCards(), game.rivalCardPlayed())));
      }
      // Bot can't beat rival's card, rival can't QYMVAM
      if (truco.canCall()) {
        return Optional.of(new BotAction.CallTruco(truco.availableCall()));
      }
      if (game.canFold()) {
        return Optional.of(new BotAction.Fold());
      }
    }

    // Escalada: si rechazar garantiza la victoria al bot, escalar aunque aceptar lo haría bust
    // (el bot se irá al mazo si el rival acepta y el encierro lo favorece)
    if (truco.canCall()) {
      final var available = truco.availableCall();
      final boolean botWinsIfRejected =
          game.myScore() + available.stakeIfRejected() == game.pointsToWin();
      final boolean highProbability =
          ctx.unplayedHand().probabilityHighCardWinsUnplayedTrick() > ACCEPT_THRESHOLD;
      if (botWinsIfRejected && highProbability) {
        return Optional.of(new BotAction.CallTruco(available));
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

  private Optional<BotAction> handleMustRespond(
      final com.villo.truco.domain.model.bot.valueobjects.BotMatchView.TrucoContext truco,
      final DecisionContext ctx) {

    if (!truco.canRespondWith(BotTrucoResponse.QUIERO)) {
      return Optional.empty();
    }
    final var pending = truco.currentCall();
    final var arithmetic = ctx.arithmetic();
    // No intervenir si ResponseToRivalCallRule ya debe manejar el caso (rival bust)
    if (arithmetic.rivalBustsIfAccepts(pending.stakeIfAccepted())
        || arithmetic.rivalBustsIfRejects(pending.stakeIfRejected())) {
      return Optional.empty();
    }
    if (ctx.unplayedHand().probabilityHighCardWinsUnplayedTrick() > ACCEPT_THRESHOLD) {
      return Optional.of(new BotAction.RespondTruco(BotTrucoResponse.QUIERO));
    }
    return Optional.empty();
  }

  private static BotCard bestBeatingCard(final List<BotCard> myCards,
      final BotCard rivalCardPlayed) {

    final int rivalRank = rivalCardPlayed.trucoRank();
    return myCards.stream()
        .filter(c -> c.trucoRank() > rivalRank)
        .min(Comparator.comparingInt(BotCard::trucoRank))
        .orElseGet(
            () -> myCards.stream().min(Comparator.comparingInt(BotCard::trucoRank)).orElseThrow());
  }

}
