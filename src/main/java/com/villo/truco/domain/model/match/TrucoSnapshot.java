package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record TrucoSnapshot(TrucoCall currentCall, PlayerId caller, int pointsAtStake) {

}
