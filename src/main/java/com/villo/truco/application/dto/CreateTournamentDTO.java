package com.villo.truco.application.dto;

import java.util.List;

public record CreateTournamentDTO(String tournamentId, List<TournamentMatchdayDTO> matchdays) {

}
