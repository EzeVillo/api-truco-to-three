package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.decision.CardLockAnalyzer;
import com.villo.truco.domain.model.bot.decision.DecisionContext;
import com.villo.truco.domain.model.bot.decision.DecisionRuleRegistry;
import com.villo.truco.domain.model.bot.decision.MatchArithmetic;
import com.villo.truco.domain.model.bot.decision.TantoProbabilityProvider;
import com.villo.truco.domain.model.bot.decision.UnplayedHandProbability;
import com.villo.truco.domain.model.bot.decision.rules.EnvidoAtTwoTwoRule;
import com.villo.truco.domain.model.bot.decision.rules.ExpectedValueFallbackRule;
import com.villo.truco.domain.model.bot.decision.rules.ForceRivalBustRule;
import com.villo.truco.domain.model.bot.decision.rules.LockAndMazoRule;
import com.villo.truco.domain.model.bot.decision.rules.ResponseToRivalCallRule;
import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class BotDecisionEngine {

  private final DecisionRuleRegistry registry;
  private final EnvidoScoring envidoScoring;
  private final BotPersonality personality;
  private final Random random;

  public BotDecisionEngine(final BotPersonality personality, final EnvidoScoring envidoScoring) {

    this(personality, new Random(), envidoScoring);
  }

  BotDecisionEngine(final BotPersonality personality, final Random random,
      final EnvidoScoring envidoScoring) {

    Objects.requireNonNull(personality);
    Objects.requireNonNull(random);
    Objects.requireNonNull(envidoScoring);
    this.personality = personality;
    this.random = random;
    this.envidoScoring = envidoScoring;
    this.registry = new DecisionRuleRegistry(
        List.of(new ResponseToRivalCallRule(), new EnvidoAtTwoTwoRule(), new ForceRivalBustRule(),
            new LockAndMazoRule(),
            new ExpectedValueFallbackRule(personality, random, envidoScoring)));
  }

  public BotAction decide(final BotMatchView view) {

    Objects.requireNonNull(view);
    final var game = view.game();
    final var arithmetic = new MatchArithmetic(game.myScore(), game.rivalScore(),
        game.pointsToWin());
    final var lock = new CardLockAnalyzer(game);
    final var tanto = new TantoProbabilityProvider(envidoScoring, game.myCards(),
        game.envidoScore(), game.isMano(), game.rivalCardPlayed());
    final var unplayedHand = new UnplayedHandProbability(game.myCards(), game.rivalCardPlayed());
    final var ctx = new DecisionContext(view, arithmetic, lock, tanto, unplayedHand);
    return registry.decide(ctx);
  }

}
