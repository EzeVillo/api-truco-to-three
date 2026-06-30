package com.villo.truco.social.domain.model.preferences;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SocialPreferences")
class SocialPreferencesTest {

  @Test
  @DisplayName("crea con recepcion de solicitudes habilitada por defecto")
  void createsWithFriendRequestsEnabledByDefault() {

    final var preferences = SocialPreferences.create(PlayerId.generate());

    assertThat(preferences.acceptsFriendRequests()).isTrue();
  }

  @Test
  @DisplayName("permite desactivar y reactivar la recepcion de solicitudes")
  void togglesAcceptsFriendRequests() {

    final var playerId = PlayerId.generate();
    final var preferences = SocialPreferences.create(playerId);

    preferences.changeAcceptsFriendRequests(false);
    assertThat(preferences.acceptsFriendRequests()).isFalse();
    assertThat(preferences.snapshot().playerId()).isEqualTo(playerId);
    assertThat(preferences.snapshot().acceptsFriendRequests()).isFalse();

    preferences.changeAcceptsFriendRequests(true);
    assertThat(preferences.acceptsFriendRequests()).isTrue();
  }

  @Test
  @DisplayName("reconstruye conservando el estado persistido")
  void reconstructsKeepingState() {

    final var playerId = PlayerId.generate();

    final var preferences = SocialPreferences.reconstruct(playerId, false);

    assertThat(preferences.getId()).isEqualTo(playerId);
    assertThat(preferences.acceptsFriendRequests()).isFalse();
  }

}
