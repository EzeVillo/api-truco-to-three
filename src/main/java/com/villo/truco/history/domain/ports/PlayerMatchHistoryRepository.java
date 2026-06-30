package com.villo.truco.history.domain.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.history.domain.model.PlayerMatchHistory;
import java.util.Optional;

public interface PlayerMatchHistoryRepository {

  void save(PlayerMatchHistory history);

  Optional<PlayerMatchHistory> findByPlayerId(PlayerId playerId);

}
