package com.villo.truco.profile.application.dto;

import java.util.List;

public record PlayerProfileDTO(List<UnlockedAchievementDTO> achievements, PlayerStatsDTO stats) {

}
