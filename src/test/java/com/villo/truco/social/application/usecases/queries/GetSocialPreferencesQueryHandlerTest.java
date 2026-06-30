package com.villo.truco.social.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.exceptions.SocialFeatureRequiresRegisteredUserException;
import com.villo.truco.social.application.queries.GetSocialPreferencesQuery;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import com.villo.truco.social.domain.ports.SocialPreferencesRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetSocialPreferencesQueryHandler")
class GetSocialPreferencesQueryHandlerTest {

  @Test
  @DisplayName("devuelve true por defecto cuando no hay preferencias guardadas")
  void returnsDefaultTrueWhenNoPreferencesStored() {

    final var playerId = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(playerId, "juancho"));
    final var preferencesRepo = mock(SocialPreferencesRepository.class);
    when(preferencesRepo.findByPlayerId(playerId)).thenReturn(Optional.empty());

    final var handler = new GetSocialPreferencesQueryHandler(new SocialUserGuard(userRepo),
        preferencesRepo);

    final var dto = handler.handle(new GetSocialPreferencesQuery(playerId.value().toString()));

    assertThat(dto.acceptsFriendRequests()).isTrue();
  }

  @Test
  @DisplayName("devuelve el estado persistido")
  void returnsStoredState() {

    final var playerId = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(playerId, "juancho"));
    final var preferencesRepo = mock(SocialPreferencesRepository.class);
    when(preferencesRepo.findByPlayerId(playerId)).thenReturn(
        Optional.of(SocialPreferences.reconstruct(playerId, false)));

    final var handler = new GetSocialPreferencesQueryHandler(new SocialUserGuard(userRepo),
        preferencesRepo);

    final var dto = handler.handle(new GetSocialPreferencesQuery(playerId.value().toString()));

    assertThat(dto.acceptsFriendRequests()).isFalse();
  }

  @Test
  @DisplayName("rechaza usuarios guest")
  void rejectsGuests() {

    final var guest = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    final var handler = new GetSocialPreferencesQueryHandler(new SocialUserGuard(userRepo),
        mock(SocialPreferencesRepository.class));

    assertThatThrownBy(
        () -> handler.handle(new GetSocialPreferencesQuery(guest.value().toString()))).isInstanceOf(
        SocialFeatureRequiresRegisteredUserException.class);
  }

}
