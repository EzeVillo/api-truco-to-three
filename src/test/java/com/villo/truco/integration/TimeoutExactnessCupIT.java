package com.villo.truco.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.ports.CupQueryRepository;
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

@SpringBootTest(properties = {"truco.cup.lobby-timeout-seconds=2"})
@ActiveProfiles("test")
@DisplayName("Timeout exacto de copa (integración)")
class TimeoutExactnessCupIT {

  @MockitoBean
  private SpringDataJoinCodeRegistryRepository springDataJoinCodeRegistryRepository;

  @Autowired
  private CupQueryRepository cupQueryRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TimeoutReconciliationRunner reconciliationRunner;

  @BeforeEach
  void setUp() {

    when(springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);
  }

  @Test
  @DisplayName("Copa inactiva es cancelada dentro de ±1 s del vencimiento programado")
  void inactiveCupTimesOutWithinTolerance() {

    final var cupId = UUID.randomUUID();
    final var pastActivityAt = Timestamp.from(Instant.now().minusSeconds(3));

    jdbcTemplate.update(
        "INSERT INTO cups (id, number_of_players, games_to_play, join_code, visibility, status, "
            + "last_activity_at, version) "
            + "VALUES (?, 4, 3, ?, 'PRIVATE', 'WAITING_FOR_PLAYERS', ?, 0)", cupId,
        "CUP-" + cupId.toString().substring(0, 6), pastActivityAt);

    final var domainCupId = new CupId(cupId);
    reconciliationRunner.reconcile("test");

    await().atMost(4, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          final var cup = cupQueryRepository.findById(domainCupId);
          assertThat(cup).isPresent();
          assertThat(cup.get().getStatus()).isEqualTo(CupStatus.CANCELLED);
        });
  }

}
