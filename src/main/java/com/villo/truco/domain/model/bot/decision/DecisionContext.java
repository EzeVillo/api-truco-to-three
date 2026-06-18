package com.villo.truco.domain.model.bot.decision;

import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import java.util.Objects;

public final class DecisionContext {

  private final BotMatchView view;
  private final MatchArithmetic arithmetic;
  private final CardLockAnalyzer lock;
  private final TantoProbabilityProvider tanto;
  private final UnplayedHandProbability unplayedHand;

  public DecisionContext(final BotMatchView view, final MatchArithmetic arithmetic,
      final CardLockAnalyzer lock, final TantoProbabilityProvider tanto,
      final UnplayedHandProbability unplayedHand) {

    this.view = Objects.requireNonNull(view);
    this.arithmetic = Objects.requireNonNull(arithmetic);
    this.lock = Objects.requireNonNull(lock);
    this.tanto = Objects.requireNonNull(tanto);
    this.unplayedHand = Objects.requireNonNull(unplayedHand);
  }

  public BotMatchView view() {

    return view;
  }

  public MatchArithmetic arithmetic() {

    return arithmetic;
  }

  public CardLockAnalyzer lock() {

    return lock;
  }

  public TantoProbabilityProvider tanto() {

    return tanto;
  }

  public UnplayedHandProbability unplayedHand() {

    return unplayedHand;
  }

}
