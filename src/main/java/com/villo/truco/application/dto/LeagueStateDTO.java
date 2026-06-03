package com.villo.truco.application.dto;

import java.util.List;

public record LeagueStateDTO(String leagueId, String status, String host, int totalSlots,
                             int occupiedSlots, boolean canStart,
                             List<LeagueParticipantDTO> participants,
                             List<LeagueStandingDTO> standings, List<String> winners,
                             List<LeagueMatchdayDTO> matchdays) {

}
