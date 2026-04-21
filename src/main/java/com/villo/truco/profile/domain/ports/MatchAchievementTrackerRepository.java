package com.villo.truco.profile.domain.ports;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.profile.domain.model.MatchAchievementTracker;
import java.util.Optional;

public interface MatchAchievementTrackerRepository {

  void save(MatchAchievementTracker tracker);

  Optional<MatchAchievementTracker> findByMatchId(MatchId matchId);

  void deleteByMatchId(MatchId matchId);
}
