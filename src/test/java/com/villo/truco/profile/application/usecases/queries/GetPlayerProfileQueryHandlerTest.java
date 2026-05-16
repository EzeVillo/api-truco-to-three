package com.villo.truco.profile.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.application.exceptions.PlayerNotFoundException;
import com.villo.truco.profile.domain.model.PlayerProfile;
import com.villo.truco.profile.domain.model.PlayerStats;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.PlayerStatsRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetPlayerProfileQueryHandler")
class GetPlayerProfileQueryHandlerTest {

  private final UserQueryRepository userQueryRepository = mock(UserQueryRepository.class);
  private final PlayerProfileRepository playerProfileRepository = mock(
      PlayerProfileRepository.class);
  private final PlayerStatsRepository playerStatsRepository = mock(PlayerStatsRepository.class);
  private final GetPlayerProfileQueryHandler handler = new GetPlayerProfileQueryHandler(
      userQueryRepository, playerProfileRepository, playerStatsRepository);

  @Test
  @DisplayName("happy path: devuelve DTO con username, logros vacios y stats en 0")
  void happyPath() {

    final var playerId = PlayerId.generate();
    when(userQueryRepository.findUserIdByUsername("juancho")).thenReturn(Optional.of(playerId));
    when(playerProfileRepository.findByPlayerId(playerId)).thenReturn(
        Optional.of(PlayerProfile.create(playerId)));
    when(playerStatsRepository.findByPlayerId(playerId)).thenReturn(
        Optional.of(PlayerStats.create(playerId)));

    final var dto = handler.handle(new GetPlayerProfileQuery("juancho"));

    assertThat(dto.username()).isEqualTo("juancho");
    assertThat(dto.achievements()).isEmpty();
    assertThat(dto.stats().matchesPlayed()).isEqualTo(0);
    assertThat(dto.stats().winRate()).isEqualTo(0);
  }

  @Test
  @DisplayName("usuario inexistente lanza PlayerNotFoundException")
  void unknownPlayerThrows404() {

    when(userQueryRepository.findUserIdByUsername("noexiste")).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> handler.handle(new GetPlayerProfileQuery("noexiste"))).isInstanceOf(
        PlayerNotFoundException.class);
  }

  @Test
  @DisplayName("profile inexistente lanza PlayerNotFoundException")
  void missingProfileThrows() {

    final var playerId = PlayerId.generate();
    when(userQueryRepository.findUserIdByUsername("testuser")).thenReturn(Optional.of(playerId));
    when(playerProfileRepository.findByPlayerId(playerId)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> handler.handle(new GetPlayerProfileQuery("testuser"))).isInstanceOf(
        PlayerNotFoundException.class);
  }

  @Test
  @DisplayName("stats inexistentes lanza PlayerNotFoundException")
  void missingStatsThrows() {

    final var playerId = PlayerId.generate();
    when(userQueryRepository.findUserIdByUsername("testuser")).thenReturn(Optional.of(playerId));
    when(playerProfileRepository.findByPlayerId(playerId)).thenReturn(
        Optional.of(PlayerProfile.create(playerId)));
    when(playerStatsRepository.findByPlayerId(playerId)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> handler.handle(new GetPlayerProfileQuery("testuser"))).isInstanceOf(
        PlayerNotFoundException.class);
  }

}
