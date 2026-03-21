package com.villo.truco.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AggregateBase")
class AggregateBaseTest {

  @Test
  void managesVersion() {

    final var aggregate = new TestAggregate("id");
    aggregate.setVersion(5);
    assertThat(aggregate.getVersion()).isEqualTo(5L);
  }

  static final class TestAggregate extends AggregateBase<String> {

    TestAggregate(String id) {

      super(id);
    }

  }

}
