package com.villo.truco.infrastructure.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import com.villo.truco.infrastructure.persistence.exceptions.JoinCodeRegistryCollisionInfrastructureException;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OptimisticLockRetryBehavior")
class OptimisticLockRetryBehaviorTest {

  private final OptimisticLockRetryBehavior behavior = new OptimisticLockRetryBehavior(3,
      Duration.ZERO);

  private static JoinCodeRegistryCollisionInfrastructureException collision() {

    return new JoinCodeRegistryCollisionInfrastructureException(JoinCode.of("ABCD1234"),
        JoinTargetType.MATCH, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        JoinTargetType.CUP, UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
  }

  @Test
  @DisplayName("éxito sin retry devuelve resultado directo")
  void successWithoutRetry() {

    final var result = behavior.handle("cmd", () -> "ok");

    assertThat(result).isEqualTo("ok");
  }

  @Test
  @DisplayName("retry exitoso tras StaleAggregateException")
  void retriesOnStaleAggregate() {

    final var attempts = new AtomicInteger(0);

    final var result = behavior.handle("cmd", () -> {
      if (attempts.incrementAndGet() < 3) {
        throw new StaleAggregateException("stale", null);
      }
      return "recovered";
    });

    assertThat(result).isEqualTo("recovered");
    assertThat(attempts.get()).isEqualTo(3);
  }

  @Test
  @DisplayName("lanza StaleAggregateException tras agotar reintentos")
  void throwsAfterMaxRetries() {

    final var attempts = new AtomicInteger(0);

    assertThatThrownBy(() -> behavior.handle("cmd", () -> {
      attempts.incrementAndGet();
      throw new StaleAggregateException("stale", null);
    })).isInstanceOf(StaleAggregateException.class);

    assertThat(attempts.get()).isEqualTo(3);
  }

  @Test
  @DisplayName("retry exitoso tras colision de join code")
  void retriesOnJoinCodeCollision() {

    final var attempts = new AtomicInteger(0);

    final var result = behavior.handle("cmd", () -> {
      if (attempts.incrementAndGet() < 3) {
        throw collision();
      }
      return "recovered";
    });

    assertThat(result).isEqualTo("recovered");
    assertThat(attempts.get()).isEqualTo(3);
  }

  @Test
  @DisplayName("lanza colision de join code tras agotar reintentos")
  void throwsJoinCodeCollisionAfterMaxRetries() {

    final var attempts = new AtomicInteger(0);

    assertThatThrownBy(() -> behavior.handle("cmd", () -> {
      attempts.incrementAndGet();
      throw collision();
    })).isInstanceOf(JoinCodeRegistryCollisionInfrastructureException.class);

    assertThat(attempts.get()).isEqualTo(3);
  }

  @Test
  @DisplayName("otras excepciones se propagan inmediatamente sin retry")
  void otherExceptionsPropagate() {

    final var attempts = new AtomicInteger(0);

    assertThatThrownBy(() -> behavior.handle("cmd", () -> {
      attempts.incrementAndGet();
      throw new IllegalArgumentException("bad input");
    })).isInstanceOf(IllegalArgumentException.class).hasMessage("bad input");

    assertThat(attempts.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("delay ocurre entre reintentos")
  void delayOccursBetweenRetries() {

    final var delayMs = 50L;
    final var behaviorWithDelay = new OptimisticLockRetryBehavior(3, Duration.ofMillis(delayMs));
    final var attempts = new AtomicInteger(0);

    final var start = System.currentTimeMillis();
    final var result = behaviorWithDelay.handle("cmd", () -> {
      if (attempts.incrementAndGet() < 3) {
        throw new StaleAggregateException("stale", null);
      }
      return "recovered";
    });
    final var elapsed = System.currentTimeMillis() - start;

    assertThat(result).isEqualTo("recovered");
    assertThat(attempts.get()).isEqualTo(3);
    assertThat(elapsed).isGreaterThanOrEqualTo(2 * delayMs);
  }

}
