package com.villo.truco.infrastructure.actuator.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.infrastructure.scheduler.SpringTimeoutScheduler;
import com.villo.truco.infrastructure.scheduler.TimeoutReconciliationRunner;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

@DisplayName("SchedulerHeartbeatHealthIndicator — estado UP/DOWN")
class SchedulerHeartbeatHealthIndicatorTest {

  private TimeoutReconciliationRunner reconciliationRunner;
  private SpringTimeoutScheduler springTimeoutScheduler;
  private SchedulerHeartbeatHealthIndicator indicator;

  @BeforeEach
  void setUp() {

    reconciliationRunner = mock(TimeoutReconciliationRunner.class);
    springTimeoutScheduler = mock(SpringTimeoutScheduler.class);
    when(springTimeoutScheduler.isPoolAlive()).thenReturn(true);
    when(springTimeoutScheduler.pendingCount()).thenReturn(0);
    indicator = new SchedulerHeartbeatHealthIndicator(reconciliationRunner, springTimeoutScheduler,
        600_000L);
  }

  @Test
  @DisplayName("UP cuando el pool está vivo y la reconciliación fue reciente")
  void upWhenPoolAliveAndReconciliationIsRecent() {

    when(reconciliationRunner.getLastReconciliationAt()).thenReturn(Instant.now().minusSeconds(30));

    final var health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  @DisplayName("DOWN cuando la última reconciliación fue hace más de 10 minutos")
  void downWhenReconciliationIsTooOld() {

    when(reconciliationRunner.getLastReconciliationAt()).thenReturn(
        Instant.now().minusSeconds(700));

    final var health = indicator.health();

    assertThat(health.getStatus()).isNotEqualTo(Status.UP);
  }

  @Test
  @DisplayName("UP con safetyNet=never-ran cuando aún no hubo reconciliación (pool vivo)")
  void upWithNeverRanWhenNoReconciliationYet() {

    when(reconciliationRunner.getLastReconciliationAt()).thenReturn(null);

    final var health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("safetyNet", "never-ran");
  }

  @Test
  @DisplayName("DOWN cuando el pool de scheduling está terminado")
  void downWhenPoolTerminated() {

    when(springTimeoutScheduler.isPoolAlive()).thenReturn(false);
    when(reconciliationRunner.getLastReconciliationAt()).thenReturn(Instant.now().minusSeconds(30));

    final var health = indicator.health();

    assertThat(health.getStatus()).isNotEqualTo(Status.UP);
  }

}
