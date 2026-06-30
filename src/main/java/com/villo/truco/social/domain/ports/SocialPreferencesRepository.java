package com.villo.truco.social.domain.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import java.util.Optional;

public interface SocialPreferencesRepository {

  void save(SocialPreferences socialPreferences);

  Optional<SocialPreferences> findByPlayerId(PlayerId playerId);

}
