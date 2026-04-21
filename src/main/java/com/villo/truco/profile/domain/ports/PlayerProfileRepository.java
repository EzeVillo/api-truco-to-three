package com.villo.truco.profile.domain.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.PlayerProfile;
import java.util.Optional;

public interface PlayerProfileRepository {

  void save(PlayerProfile profile);

  Optional<PlayerProfile> findByPlayerId(PlayerId playerId);
}
