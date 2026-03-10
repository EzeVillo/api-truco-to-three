package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import java.util.List;

final class EnvidoRaiseSpecification {

  private EnvidoRaiseSpecification() {

  }

  static boolean isSatisfiedBy(final List<EnvidoCall> chain, final EnvidoCall call) {

    if (chain.isEmpty()) {
      return true;
    }

    final var lastCall = chain.getLast();
    if (!lastCall.canBeRaisedWith(call)) {
      return false;
    }

    if (call == EnvidoCall.ENVIDO) {
      final var envidoCount = chain.stream().filter(c -> c == EnvidoCall.ENVIDO).count();
      return envidoCount < 2;
    }

    return true;
  }

}