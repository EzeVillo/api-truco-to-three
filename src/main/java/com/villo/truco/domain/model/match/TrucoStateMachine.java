package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.exceptions.InvalidTrucoCallException;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

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

    return TrucoEscalationSpecification.isSatisfiedBy(this.currentCall, this.caller, playerId);
  }

  public TrucoCall call(final PlayerId playerId) {

    if (!TrucoEscalationSpecification.isSatisfiedBy(this.currentCall, this.caller, playerId)) {
      throw new InvalidTrucoCallException();
    }

    this.currentCall = TrucoEscalationSpecification.nextCall(this.currentCall);
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

  void initializeState(final TrucoCall currentCall, final PlayerId caller,
      final int pointsAtStake) {

    this.currentCall = currentCall;
    this.caller = caller;
    this.pointsAtStake = pointsAtStake;
  }

}
