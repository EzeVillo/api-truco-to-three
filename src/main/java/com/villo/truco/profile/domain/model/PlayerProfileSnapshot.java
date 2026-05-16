package com.villo.truco.profile.domain.model;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public record PlayerProfileSnapshot(PlayerId playerId,
                                    List<UnlockedAchievement> unlockedAchievements) {

}
