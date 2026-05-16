package com.villo.truco.profile.application.dto;

import com.villo.truco.profile.domain.model.AchievementCode;
import java.time.Instant;
import java.util.UUID;

public record UnlockedAchievementDTO(AchievementCode achievementCode, Instant unlockedAt,
                                     UUID matchId, int gameNumber) {

}
