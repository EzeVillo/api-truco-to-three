package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import org.junit.jupiter.api.Test;

class MatchActionSpecificationTest {

  @Test
  void allowsActionsOnlyWhenMatchInProgress() {

    assertThat(MatchActionSpecification.canExecuteInRound(MatchStatus.IN_PROGRESS)).isTrue();
    assertThat(
        MatchActionSpecification.canExecuteInRound(MatchStatus.WAITING_FOR_PLAYERS)).isFalse();
    assertThat(MatchActionSpecification.canExecuteInRound(MatchStatus.FINISHED)).isFalse();
  }

}