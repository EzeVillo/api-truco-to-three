package com.villo.truco.domain.model.cup.valueobjects;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record BoutPairing(BoutId boutId, PlayerId playerOne, PlayerId playerTwo) {

}
