package com.villo.truco.social.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.commands.UpdateSocialPreferencesCommand;
import com.villo.truco.social.application.exceptions.SocialFeatureRequiresRegisteredUserException;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import com.villo.truco.social.domain.ports.SocialPreferencesRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("UpdateSocialPreferencesCommandHandler")
class UpdateSocialPreferencesCommandHandlerTest {

  @Test
  @DisplayName("crea las preferencias cuando no existian y persiste el nuevo valor")
  void createsPreferencesWhenMissing() {

    final var playerId = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(playerId, "juancho"));
    final var preferencesRepo = mock(SocialPreferencesRepository.class);
    when(preferencesRepo.findByPlayerId(playerId)).thenReturn(Optional.empty());

    final var handler = new UpdateSocialPreferencesCommandHandler(new SocialUserGuard(userRepo),
        preferencesRepo);

    final var dto = handler.handle(
        new UpdateSocialPreferencesCommand(playerId.value().toString(), false));

    assertThat(dto.acceptsFriendRequests()).isFalse();
    final var captor = ArgumentCaptor.forClass(SocialPreferences.class);
    verify(preferencesRepo).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(playerId);
    assertThat(captor.getValue().acceptsFriendRequests()).isFalse();
  }

  @Test
  @DisplayName("actualiza las preferencias existentes")
  void updatesExistingPreferences() {

    final var playerId = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(playerId, "juancho"));
    final var preferencesRepo = mock(SocialPreferencesRepository.class);
    when(preferencesRepo.findByPlayerId(playerId)).thenReturn(
        Optional.of(SocialPreferences.reconstruct(playerId, false)));

    final var handler = new UpdateSocialPreferencesCommandHandler(new SocialUserGuard(userRepo),
        preferencesRepo);

    final var dto = handler.handle(
        new UpdateSocialPreferencesCommand(playerId.value().toString(), true));

    assertThat(dto.acceptsFriendRequests()).isTrue();
    final var captor = ArgumentCaptor.forClass(SocialPreferences.class);
    verify(preferencesRepo).save(captor.capture());
    assertThat(captor.getValue().acceptsFriendRequests()).isTrue();
  }

  @Test
  @DisplayName("rechaza usuarios guest")
  void rejectsGuests() {

    final var guest = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    final var handler = new UpdateSocialPreferencesCommandHandler(new SocialUserGuard(userRepo),
        mock(SocialPreferencesRepository.class));

    assertThatThrownBy(() -> handler.handle(
        new UpdateSocialPreferencesCommand(guest.value().toString(), false))).isInstanceOf(
        SocialFeatureRequiresRegisteredUserException.class);
  }

}
