package com.villo.truco.profile.application.dto;

import java.util.List;

public record PlayerProfileDTO(String username,
                               List<UnlockedAchievementDTO> achievements, PlayerStatsDTO stats) {

}
