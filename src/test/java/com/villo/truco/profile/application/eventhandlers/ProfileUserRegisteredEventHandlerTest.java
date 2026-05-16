package com.villo.truco.profile.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.auth.domain.model.user.events.UserRegisteredEvent;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.PlayerProfile;
import com.villo.truco.profile.domain.model.PlayerStats;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.PlayerStatsRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfileUserRegisteredEventHandler")
class ProfileUserRegisteredEventHandlerTest {

  @Test
  @DisplayName("crea PlayerProfile y PlayerStats vacios al registrarse un usuario")
  void createsProfileAndStatsOnUserRegistered() {

    final var profileRepo = new InMemoryPlayerProfileRepository();
    final var statsRepo = new InMemoryPlayerStatsRepository();
    final var handler = new ProfileUserRegisteredEventHandler(profileRepo, statsRepo);
    final var playerId = PlayerId.generate();

    handler.handle(new UserRegisteredEvent(playerId, new Username("testuser"), Instant.now()));

    assertThat(profileRepo.findByPlayerId(playerId)).isPresent();
    assertThat(statsRepo.findByPlayerId(playerId)).hasValueSatisfying(s -> {
      assertThat(s.matchesPlayed()).isEqualTo(0);
      assertThat(s.matchesWon()).isEqualTo(0);
      assertThat(s.matchesLost()).isEqualTo(0);
    });
  }

  private static final class InMemoryPlayerProfileRepository implements PlayerProfileRepository {

    private final Map<PlayerId, PlayerProfile> byId = new HashMap<>();

    @Override
    public void save(final PlayerProfile profile) {

      this.byId.put(profile.getId(), profile);
    }

    @Override
    public Optional<PlayerProfile> findByPlayerId(final PlayerId playerId) {

      return Optional.ofNullable(this.byId.get(playerId));
    }

  }

  private static final class InMemoryPlayerStatsRepository implements PlayerStatsRepository {

    private final Map<PlayerId, PlayerStats> byId = new HashMap<>();

    @Override
    public void save(final PlayerStats stats) {

      this.byId.put(stats.getId(), stats);
    }

    @Override
    public Optional<PlayerStats> findByPlayerId(final PlayerId playerId) {

      return Optional.ofNullable(this.byId.get(playerId));
    }

  }

}
