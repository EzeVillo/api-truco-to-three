package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import org.junit.jupiter.api.Test;

class RoundActionStatusSpecificationTest {

  @Test
  void allowsPlayingActionsOnlyInPlayingState() {

    assertThat(RoundActionStatusSpecification.canPlayCard(RoundStatus.PLAYING)).isTrue();
    assertThat(RoundActionStatusSpecification.canPlayCard(RoundStatus.TRUCO_IN_PROGRESS)).isFalse();
    assertThat(
        RoundActionStatusSpecification.canPlayCard(RoundStatus.ENVIDO_IN_PROGRESS)).isFalse();
  }

  @Test
  void allowsCallingTrucoInPlayingOrTrucoInProgress() {

    assertThat(RoundActionStatusSpecification.canCallTruco(RoundStatus.PLAYING)).isTrue();
    assertThat(RoundActionStatusSpecification.canCallTruco(RoundStatus.TRUCO_IN_PROGRESS)).isTrue();
    assertThat(
        RoundActionStatusSpecification.canCallTruco(RoundStatus.ENVIDO_IN_PROGRESS)).isFalse();
  }

  @Test
  void allowsRespondingEnvidoOnlyInEnvidoInProgress() {

    assertThat(
        RoundActionStatusSpecification.canRespondEnvido(RoundStatus.ENVIDO_IN_PROGRESS)).isTrue();
    assertThat(RoundActionStatusSpecification.canRespondEnvido(RoundStatus.PLAYING)).isFalse();
  }

}