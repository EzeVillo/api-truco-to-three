package com.villo.truco.application.dto;

import java.util.List;

public record LeagueStateDTO(String leagueId, String status, List<LeagueStandingDTO> standings,
                             List<String> winners, List<LeagueMatchdayDTO> matchdays) {

}
