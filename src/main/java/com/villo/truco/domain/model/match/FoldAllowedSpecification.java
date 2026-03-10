package com.villo.truco.domain.model.match;

final class FoldAllowedSpecification {

  private FoldAllowedSpecification() {

  }

  static boolean isSatisfiedBy(final boolean isMano, final boolean isFirstHand,
      final boolean isEnvidoResolved, final boolean isTrucoCalled) {

    return !isMano || !isFirstHand || isEnvidoResolved || isTrucoCalled;
  }

}