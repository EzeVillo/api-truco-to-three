package com.villo.truco.application.dto;

import java.util.List;

public record LeagueMatchdayDTO(int matchdayNumber, List<LeagueFixtureDTO> fixtures) {

}
