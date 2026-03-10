package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import java.util.List;

final class RoundActionStatusSpecification {

  private RoundActionStatusSpecification() {

  }

  static boolean canPlayCard(final RoundStatus status) {

    return status == RoundStatus.PLAYING;
  }

  static boolean canCallTruco(final RoundStatus status) {

    return status == RoundStatus.PLAYING || status == RoundStatus.TRUCO_IN_PROGRESS;
  }

  static boolean canRespondTruco(final RoundStatus status) {

    return status == RoundStatus.TRUCO_IN_PROGRESS;
  }

  static boolean canFold(final RoundStatus status) {

    return status == RoundStatus.PLAYING;
  }

  static boolean canRespondEnvido(final RoundStatus status) {

    return status == RoundStatus.ENVIDO_IN_PROGRESS;
  }

  static List<RoundStatus> trucoCallAllowedStatuses() {

    return List.of(RoundStatus.PLAYING, RoundStatus.TRUCO_IN_PROGRESS);
  }

}