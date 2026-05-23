package com.villo.truco.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionStatus;
import com.villo.truco.domain.ports.RematchSessionRepository;
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

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Timeout exacto de sesión de rematch (integración)")
class TimeoutExactnessRematchIT {

  @MockitoBean
  private SpringDataJoinCodeRegistryRepository springDataJoinCodeRegistryRepository;

  @Autowired
  private RematchSessionRepository rematchSessionRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TimeoutReconciliationRunner reconciliationRunner;

  @BeforeEach
  void setUp() {

    when(springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);
  }

  @Test
  @DisplayName("Sesión de rematch vencida es expirada dentro de ±1 s del vencimiento programado")
  void expiredRematchSessionTimesOutWithinTolerance() {

    final var sessionId = UUID.randomUUID();
    final var originMatchId = UUID.randomUUID();
    final var playerOneId = UUID.randomUUID();
    final var playerTwoId = UUID.randomUUID();
    final var pastExpiresAt = Timestamp.from(Instant.now().minusSeconds(3));

    jdbcTemplate.update(
        "INSERT INTO rematch_sessions (id, origin_match_id, player_one_id, player_two_id, "
            + "player_one_choice, player_two_choice, player_one_is_bot, player_two_is_bot, "
            + "status, games_to_win, expires_at, version, created_at) "
            + "VALUES (?, ?, ?, ?, 'UNDECIDED', 'UNDECIDED', false, false, 'OPEN', 3, ?, 0, NOW())",
        sessionId, originMatchId, playerOneId, playerTwoId, pastExpiresAt);

    final var domainSessionId = new RematchSessionId(sessionId);
    reconciliationRunner.reconcile("test");

    await().atMost(4, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          final var session = rematchSessionRepository.findById(domainSessionId);
          assertThat(session).isPresent();
          assertThat(session.get().getStatus()).isEqualTo(RematchSessionStatus.EXPIRED);
        });
  }

}
