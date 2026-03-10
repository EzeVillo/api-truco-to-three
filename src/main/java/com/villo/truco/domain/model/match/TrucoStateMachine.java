package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.exceptions.InvalidTrucoCallException;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;

final class TrucoStateMachine {

  private TrucoCall currentCall;
  private PlayerId caller;
  private int pointsAtStake = 1;

  public TrucoStateMachine() {

  }

  public boolean hasBeenCalled() {

    return this.currentCall != null;
  }

  public TrucoCall getCurrentCall() {

    return this.currentCall;
  }

  public PlayerId getCaller() {

    return this.caller;
  }

  public int getPointsAtStake() {

    return this.pointsAtStake;
  }

  public boolean canEscalate(final PlayerId playerId) {

    if (this.currentCall == null) {
      return true;
    }
    return this.currentCall.hasNext() && !playerId.equals(this.caller);
  }

  public TrucoCall call(final PlayerId playerId) {

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

  public void accept() {

    this.pointsAtStake = this.currentCall.pointsIfAccepted();
  }

  public int pointsIfRejected() {

    return this.currentCall.pointsIfRejected();
  }

  public int pointsIfAccepted() {

    return this.currentCall.pointsIfAccepted();
  }

  public void cancel() {

    this.currentCall = null;
    this.caller = null;
  }

}
