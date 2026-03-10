package com.villo.truco.domain.model.match;

final class MatchReadyPolicy {

  private MatchReadyPolicy() {

  }

  static ReadyState markReady(final boolean currentReadyPlayerOne,
      final boolean currentReadyPlayerTwo, final boolean requesterIsPlayerOne) {

    final var nextReadyPlayerOne = requesterIsPlayerOne || currentReadyPlayerOne;
    final var nextReadyPlayerTwo = !requesterIsPlayerOne || currentReadyPlayerTwo;

    final var alreadyReady = requesterIsPlayerOne ? currentReadyPlayerOne : currentReadyPlayerTwo;

    return new ReadyState(nextReadyPlayerOne, nextReadyPlayerTwo, !alreadyReady,
        nextReadyPlayerOne && nextReadyPlayerTwo);
  }

  record ReadyState(boolean readyPlayerOne, boolean readyPlayerTwo, boolean changed,
                    boolean bothReady) {

  }

}
