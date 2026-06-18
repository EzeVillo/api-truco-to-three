package com.villo.truco.domain.model.bot.decision;

import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DecisionRuleRegistry {

  private final List<DecisionRule> rules;

  public DecisionRuleRegistry(final List<DecisionRule> rules) {

    final var sorted = new ArrayList<>(rules);
    sorted.sort(Comparator.comparingInt(DecisionRule::priority));
    this.rules = List.copyOf(sorted);
  }

  public BotAction decide(final DecisionContext ctx) {

    for (final var rule : rules) {
      final var result = rule.apply(ctx);
      if (result.isPresent()) {
        return result.get();
      }
    }
    throw new IllegalStateException(
        "DecisionRuleRegistry no tiene regla de fallback; siempre debe haber al menos una.");
  }

}
