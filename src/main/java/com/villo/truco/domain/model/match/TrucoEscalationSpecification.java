package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

final class TrucoEscalationSpecification {

  private TrucoEscalationSpecification() {

  }

  static boolean isSatisfiedBy(final TrucoCall currentCall, final PlayerId caller,
      final PlayerId requester) {

    if (currentCall == null) {
      return true;
    }

    return currentCall.hasNext() && !requester.equals(caller);
  }

  static TrucoCall nextCall(final TrucoCall currentCall) {

    return currentCall == null ? TrucoCall.TRUCO : currentCall.next();
  }

}