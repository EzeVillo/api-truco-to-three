package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnvidoRaiseSpecificationTest {

  @Test
  void allowsAnyFirstCall() {

    final var chain = List.<EnvidoCall>of();

    assertThat(EnvidoRaiseSpecification.isSatisfiedBy(chain, EnvidoCall.ENVIDO)).isTrue();
    assertThat(EnvidoRaiseSpecification.isSatisfiedBy(chain, EnvidoCall.REAL_ENVIDO)).isTrue();
    assertThat(EnvidoRaiseSpecification.isSatisfiedBy(chain, EnvidoCall.FALTA_ENVIDO)).isTrue();
  }

  @Test
  void blocksThirdEnvido() {

    final var chain = new ArrayList<EnvidoCall>();
    chain.add(EnvidoCall.ENVIDO);
    chain.add(EnvidoCall.ENVIDO);

    assertThat(EnvidoRaiseSpecification.isSatisfiedBy(chain, EnvidoCall.ENVIDO)).isFalse();
  }

  @Test
  void blocksAnyRaiseAfterFaltaEnvido() {

    final var chain = List.of(EnvidoCall.FALTA_ENVIDO);

    assertThat(EnvidoRaiseSpecification.isSatisfiedBy(chain, EnvidoCall.ENVIDO)).isFalse();
    assertThat(EnvidoRaiseSpecification.isSatisfiedBy(chain, EnvidoCall.REAL_ENVIDO)).isFalse();
    assertThat(EnvidoRaiseSpecification.isSatisfiedBy(chain, EnvidoCall.FALTA_ENVIDO)).isFalse();
  }

}