package com.villo.truco.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainEventBase")
class DomainEventBaseTest {

  @Test
  void exposesTypeAndTimestamp() {

    final var event = new TestEvent("X");
    assertThat(event.getEventType()).isEqualTo("X");
    assertThat(event.getTimestamp()).isGreaterThan(0L);
  }

  static final class TestEvent extends DomainEventBase {

    TestEvent(String eventType) {

      super(eventType);
    }

  }

}
