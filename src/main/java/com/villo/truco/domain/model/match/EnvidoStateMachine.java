package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.exceptions.EnvidoNotAllowedException;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class EnvidoStateMachine {

  private static final int POINTS_TO_WIN_GAME = 3;

  private final List<EnvidoCall> chain = new ArrayList<>();
  private boolean resolved;

  public EnvidoStateMachine() {

  }

  public boolean isResolved() {

    return this.resolved;
  }

  public boolean isEmpty() {

    return this.chain.isEmpty();
  }

  public List<EnvidoCall> getChain() {

    return Collections.unmodifiableList(this.chain);
  }

  public boolean canRaiseWith(final EnvidoCall call) {

    return EnvidoRaiseSpecification.isSatisfiedBy(this.chain, call);
  }

  public void call(final EnvidoCall call) {

    if (!EnvidoRaiseSpecification.isSatisfiedBy(this.chain, call)) {

      if (!this.chain.isEmpty() && !this.chain.getLast().canBeRaisedWith(call)) {
        final var lastCall = this.chain.getLast();
        throw new EnvidoNotAllowedException("No se puede responder " + lastCall + " con " + call);
      }

      if (call == EnvidoCall.ENVIDO) {
        throw new EnvidoNotAllowedException("No se puede cantar más de dos envidos");
      }

      throw new EnvidoNotAllowedException("No se puede cantar envido en este momento");
    }

    this.chain.add(call);
  }

  public void resolve() {

    this.resolved = true;
  }

  public int calculateAcceptedPoints(final int scorePlayerOne, final int scorePlayerTwo,
      final PlayerId winner, final PlayerId playerOne) {

    final var hasFaltaEnvido = this.chain.contains(EnvidoCall.FALTA_ENVIDO);

    if (hasFaltaEnvido) {
      final var rivalScore = winner.equals(playerOne) ? scorePlayerTwo : scorePlayerOne;
      return POINTS_TO_WIN_GAME - rivalScore;
    }

    return this.chain.stream().mapToInt(EnvidoCall::points).sum();
  }

  public int calculateRejectedPoints() {

    if (this.chain.size() == 1) {
      return 1;
    }

    final var chainWithoutLast = this.chain.subList(0, this.chain.size() - 1);
    return chainWithoutLast.stream().filter(call -> call != EnvidoCall.FALTA_ENVIDO)
        .mapToInt(EnvidoCall::points).sum();
  }

}
