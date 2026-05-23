package com.villo.truco.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionStatus;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataJoinCodeRegistryRepository;
import com.villo.truco.infrastructure.scheduler.SpringTimeoutScheduler;
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
@DisplayName("TimeoutReconciliationIT — reconciliación al arranque")
class TimeoutReconciliationIT {

  @MockitoBean
  private SpringDataJoinCodeRegistryRepository springDataJoinCodeRegistryRepository;

  @Autowired
  private RematchSessionRepository rematchSessionRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TimeoutReconciliationRunner reconciliationRunner;

  @Autowired
  private SpringTimeoutScheduler springTimeoutScheduler;

  @BeforeEach
  void setUp() {

    when(springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);
  }

  @Test
  @DisplayName("Sesión vencida se expira y sesión futura queda programada tras reconciliación")
  void expiredSessionIsProcessedAndFutureSessionRemainsScheduled() {

    final var expiredSessionId = UUID.randomUUID();
    final var futureSessionId = UUID.randomUUID();
    final var originMatchId = UUID.randomUUID();
    final var playerOneId = UUID.randomUUID();
    final var playerTwoId = UUID.randomUUID();

    final var pastExpiresAt = Timestamp.from(Instant.now().minusSeconds(5));
    final var futureExpiresAt = Timestamp.from(Instant.now().plusSeconds(60));

    jdbcTemplate.update(
        "INSERT INTO rematch_sessions (id, origin_match_id, player_one_id, player_two_id, "
            + "player_one_choice, player_two_choice, player_one_is_bot, player_two_is_bot, "
            + "status, games_to_win, expires_at, version, created_at) "
            + "VALUES (?, ?, ?, ?, 'UNDECIDED', 'UNDECIDED', false, false, 'OPEN', 3, ?, 0, NOW())",
        expiredSessionId, originMatchId, playerOneId, playerTwoId, pastExpiresAt);

    final var futureOriginMatchId = UUID.randomUUID();
    final var futurePlayerOneId = UUID.randomUUID();
    final var futurePlayerTwoId = UUID.randomUUID();

    jdbcTemplate.update(
        "INSERT INTO rematch_sessions (id, origin_match_id, player_one_id, player_two_id, "
            + "player_one_choice, player_two_choice, player_one_is_bot, player_two_is_bot, "
            + "status, games_to_win, expires_at, version, created_at) "
            + "VALUES (?, ?, ?, ?, 'UNDECIDED', 'UNDECIDED', false, false, 'OPEN', 3, ?, 0, NOW())",
        futureSessionId, futureOriginMatchId, futurePlayerOneId, futurePlayerTwoId,
        futureExpiresAt);

    reconciliationRunner.reconcile("test");

    final var domainExpiredId = new RematchSessionId(expiredSessionId);
    final var domainFutureId = new RematchSessionId(futureSessionId);
    final var futureKey = TimeoutKey.of(EntityType.REMATCH_SESSION, futureSessionId.toString());

    await().atMost(5, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          final var expiredSession = rematchSessionRepository.findById(domainExpiredId);
          assertThat(expiredSession).isPresent();
          assertThat(expiredSession.get().getStatus()).isEqualTo(RematchSessionStatus.EXPIRED);
        });

    final var futureSession = rematchSessionRepository.findById(domainFutureId);
    assertThat(futureSession).isPresent();
    assertThat(futureSession.get().getStatus()).isEqualTo(RematchSessionStatus.OPEN);

    assertThat(springTimeoutScheduler.isPending(futureKey)).isTrue();
  }

}
