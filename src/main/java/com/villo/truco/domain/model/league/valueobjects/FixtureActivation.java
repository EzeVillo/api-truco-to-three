package com.villo.truco.domain.model.league.valueobjects;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record FixtureActivation(FixtureId fixtureId, PlayerId playerOne, PlayerId playerTwo) {

}
