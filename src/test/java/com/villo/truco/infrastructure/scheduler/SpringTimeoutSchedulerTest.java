package com.villo.truco.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class SpringTimeoutSchedulerTest {

  private static final TimeoutKey KEY = TimeoutKey.of(EntityType.MATCH, "test-id-1");

  private SpringTimeoutScheduler schedulerWithFixedClock(final Instant now) {

    final var clock = Clock.fixed(now, ZoneOffset.UTC);
    final var taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(2);
    taskScheduler.initialize();
    final TimeoutMetrics metrics = new TimeoutMetrics(new SimpleMeterRegistry());
    return new SpringTimeoutScheduler(taskScheduler, clock, metrics);
  }

  @Test
  @DisplayName("debe programar action para deadline futuro")
  void debeProgramarActionParaDeadlineFuturo() throws InterruptedException {

    final var now = Instant.now();
    final var scheduler = schedulerWithFixedClock(now);
    final var executed = new AtomicInteger(0);

    scheduler.schedule(KEY, now.plus(Duration.ofMillis(100)), executed::incrementAndGet);

    assertThat(scheduler.isPending(KEY)).isTrue();
    Thread.sleep(300);
    assertThat(executed.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("debe ejecutar inmediatamente si deadline ya pasó")
  void debeEjecutarInmediatamenteSiDeadlineYaPaso() throws InterruptedException {

    final var now = Instant.now();
    final var scheduler = schedulerWithFixedClock(now);
    final var executed = new AtomicInteger(0);

    scheduler.schedule(KEY, now.minus(Duration.ofSeconds(10)), executed::incrementAndGet);

    Thread.sleep(200);
    assertThat(executed.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("debe cancelar future previo al reprogramar misma key")
  void debeCancelarFuturePrevioAlReprogramarMismaKey() throws InterruptedException {

    final var now = Instant.now();
    final var scheduler = schedulerWithFixedClock(now);
    final var counter = new AtomicInteger(0);

    scheduler.schedule(KEY, now.plus(Duration.ofSeconds(10)), counter::incrementAndGet);
    scheduler.schedule(KEY, now.plus(Duration.ofMillis(100)), counter::incrementAndGet);

    Thread.sleep(300);
    assertThat(counter.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("debe ser no-op al cancelar key inexistente")
  void debeSerNoOpAlCancelarKeyInexistente() {

    final var scheduler = schedulerWithFixedClock(Instant.now());

    Assertions.assertDoesNotThrow(
        () -> scheduler.cancel(TimeoutKey.of(EntityType.CUP, "no-existe")));
  }

  @Test
  @DisplayName("isPending refleja estado correcto")
  void isPendingReflejaEstadoCorrecto() {

    final var now = Instant.now();
    final var scheduler = schedulerWithFixedClock(now);
    final var executed = new AtomicInteger(0);

    assertThat(scheduler.isPending(KEY)).isFalse();

    scheduler.schedule(KEY, now.plus(Duration.ofSeconds(5)), executed::incrementAndGet);
    assertThat(scheduler.isPending(KEY)).isTrue();

    scheduler.cancel(KEY);
    assertThat(scheduler.isPending(KEY)).isFalse();
  }

}
