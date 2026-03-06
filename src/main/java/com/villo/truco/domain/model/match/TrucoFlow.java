package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.exceptions.InvalidTrucoCallException;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;

final class TrucoFlow {

  private TrucoCall currentCall;
  private PlayerId caller;
  private int pointsAtStake = 1;

  TrucoFlow() {

  }

  boolean hasBeenCalled() {

    return this.currentCall != null;
  }

  TrucoCall getCurrentCall() {

    return this.currentCall;
  }

  PlayerId getCaller() {

    return this.caller;
  }

  int getPointsAtStake() {

    return this.pointsAtStake;
  }

  boolean canEscalate(final PlayerId playerId) {

    if (this.currentCall == null) {
      return true;
    }
    return this.currentCall.hasNext() && !playerId.equals(this.caller);
  }

  TrucoCall call(final PlayerId playerId) {

    if (this.currentCall == null) {
      this.currentCall = TrucoCall.TRUCO;
    } else if (this.currentCall.hasNext() && !playerId.equals(this.caller)) {
      this.currentCall = this.currentCall.next();
    } else {
      throw new InvalidTrucoCallException();
    }
    this.caller = playerId;
    return this.currentCall;
  }

  void accept() {

    this.pointsAtStake = this.currentCall.pointsIfAccepted();
  }

  int pointsIfRejected() {

    return this.currentCall.pointsIfRejected();
  }

  int pointsIfAccepted() {

    return this.currentCall.pointsIfAccepted();
  }

  void cancel() {

    this.currentCall = null;
    this.caller = null;
  }

}
