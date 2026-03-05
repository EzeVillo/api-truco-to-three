package com.villo.truco.application.dto;

import java.util.List;

public record TournamentStateDTO(String tournamentId, String status,
                                 List<TournamentStandingDTO> standings, List<String> winners,
                                 List<TournamentMatchdayDTO> matchdays) {

}
