package com.villo.truco.domain.model.match.valueobjects;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record EnvidoResult(int pointsMano, int pointsPie, PlayerId winner, int pointsWon) {

}
