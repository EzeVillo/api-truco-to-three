package com.villo.truco.profile.domain.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.PlayerStats;
import java.util.Optional;

public interface PlayerStatsRepository {

  void save(PlayerStats stats);

  Optional<PlayerStats> findByPlayerId(PlayerId playerId);

}
