package com.villo.truco.application.dto;

import java.util.List;

public record TournamentMatchdayDTO(int matchdayNumber, List<TournamentFixtureDTO> fixtures) {

}
