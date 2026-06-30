package com.villo.truco.social.domain.model.friendship;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.exceptions.FriendLimitReachedException;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FriendshipLimitPolicy")
class FriendshipLimitPolicyTest {

  @Test
  @DisplayName("permite agregar cuando el jugador está por debajo del límite")
  void allowsWhenBelowLimit() {

    final var playerId = PlayerId.generate();
    final var repo = mock(FriendshipQueryRepository.class);
    when(repo.countAcceptedByPlayer(playerId)).thenReturn(9);
    final var policy = new FriendshipLimitPolicy(repo, 10);

    assertThatCode(() -> policy.ensureSelfHasRoom(playerId)).doesNotThrowAnyException();
    assertThatCode(() -> policy.ensureCounterpartHasRoom(playerId)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("rechaza al propio jugador cuando ya alcanzó el límite")
  void rejectsSelfWhenAtLimit() {

    final var playerId = PlayerId.generate();
    final var repo = mock(FriendshipQueryRepository.class);
    when(repo.countAcceptedByPlayer(playerId)).thenReturn(10);
    final var policy = new FriendshipLimitPolicy(repo, 10);

    assertThatThrownBy(() -> policy.ensureSelfHasRoom(playerId)).isInstanceOf(
        FriendLimitReachedException.class);
  }

  @Test
  @DisplayName("rechaza a la contraparte cuando ya alcanzó el límite")
  void rejectsCounterpartWhenAtLimit() {

    final var playerId = PlayerId.generate();
    final var repo = mock(FriendshipQueryRepository.class);
    when(repo.countAcceptedByPlayer(playerId)).thenReturn(11);
    final var policy = new FriendshipLimitPolicy(repo, 10);

    assertThatThrownBy(() -> policy.ensureCounterpartHasRoom(playerId)).isInstanceOf(
        FriendLimitReachedException.class);
  }

  @Test
  @DisplayName("rechaza configuración con un máximo menor a 1")
  void rejectsInvalidMaxFriends() {

    final var repo = mock(FriendshipQueryRepository.class);

    assertThatThrownBy(() -> new FriendshipLimitPolicy(repo, 0)).isInstanceOf(
        IllegalArgumentException.class);
  }

}
