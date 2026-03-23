package com.villo.truco.domain.model.league;

import java.util.List;

public record MatchdayView(int matchdayNumber, List<FixtureView> fixtures) {

}
