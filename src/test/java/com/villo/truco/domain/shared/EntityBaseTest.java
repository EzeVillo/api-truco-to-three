package com.villo.truco.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntityBase")
class EntityBaseTest {

  @Test
  void managesEventsAndEqualityById() {

    final var e1 = new TestEntity("id");
    final var e2 = new TestEntity("id");
    e1.addDomainEvent(new TestEvent("A"));

    assertThat(e1.getDomainEvents()).hasSize(1);
    e1.clearDomainEvents();
    assertThat(e1.getDomainEvents()).isEmpty();
    assertThat(e1).isEqualTo(e2);
  }

  static final class TestEvent extends DomainEventBase {

    TestEvent(String eventType) {

      super(eventType);
    }

  }

  static final class TestEntity extends EntityBase<String> {

    TestEntity(String id) {

      super(id);
    }

  }

}
