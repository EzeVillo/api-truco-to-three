package com.villo.truco.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataJoinCodeRegistryRepository;
import com.villo.truco.infrastructure.scheduler.TimeoutReconciliationRunner;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {"truco.match.idle-timeout-seconds=2"})
@ActiveProfiles("test")
@DisplayName("Timeout exacto de partida (integración)")
class TimeoutExactnessMatchIT {

  @MockitoBean
  private SpringDataJoinCodeRegistryRepository springDataJoinCodeRegistryRepository;

  @Autowired
  private MatchQueryRepository matchQueryRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TimeoutReconciliationRunner reconciliationRunner;

  @BeforeEach
  void setUp() {

    when(springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);
  }

  @Test
  @DisplayName("Partida inactiva es cancelada dentro de ±1 s del vencimiento programado")
  void inactiveMatchTimesOutWithinTolerance() {

    final var matchId = UUID.randomUUID();
    final var playerId = UUID.randomUUID();
    final var pastActivityAt = Timestamp.from(Instant.now().minusSeconds(3));

    jdbcTemplate.update(
        "INSERT INTO matches (id, player_one, player_two, join_code, visibility, status, "
            + "games_to_win, games_won_player_one, games_won_player_two, game_number, "
            + "score_player_one, score_player_two, round_number, "
            + "ready_player_one, ready_player_two, last_activity_at, version, state_version) "
            + "VALUES (?, ?, null, ?, 'PRIVATE', 'WAITING_FOR_PLAYERS', 3, 0, 0, 0, 0, 0, 0, false, false, ?, 0, 0)",
        matchId, playerId, "TEST-" + matchId.toString().substring(0, 6), pastActivityAt);

    final var domainMatchId = new MatchId(matchId);
    reconciliationRunner.reconcile("test");

    await().atMost(4, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          final var match = matchQueryRepository.findById(domainMatchId);
          assertThat(match).isPresent();
          assertThat(match.get().getStatus()).isEqualTo(MatchStatus.CANCELLED);
        });
  }

}
