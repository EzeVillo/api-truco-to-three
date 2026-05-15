package com.villo.truco.domain.model.match;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class RoundTerminationPolicy {

  private RoundTerminationPolicy() {

  }

  static Optional<PlayerId> resolveWinner(final List<PlayerId> handWinners,
      final PlayerId playerOne, final PlayerId playerTwo, final PlayerId mano) {

    Objects.requireNonNull(handWinners, "Hand winners cannot be null");
    Objects.requireNonNull(playerOne, "Player one cannot be null");
    Objects.requireNonNull(playerTwo, "Player two cannot be null");
    Objects.requireNonNull(mano, "Mano cannot be null");

    final int winsPlayerOne = countWins(handWinners, playerOne);
    final int winsPlayerTwo = countWins(handWinners, playerTwo);

    if (winsPlayerOne >= 2) {
      return Optional.of(playerOne);
    }

    if (winsPlayerTwo >= 2) {
      return Optional.of(playerTwo);
    }

    final var firstNonTie = handWinners.stream().filter(Objects::nonNull).findFirst();

    if (handWinners.size() == 2) {
      final var hasOneTie = handWinners.stream().anyMatch(Objects::isNull);
      if (firstNonTie.isPresent() && hasOneTie) {
        return firstNonTie;
      }
      return Optional.empty();
    }

    if (handWinners.size() == 3) {
      return Optional.of(firstNonTie.orElse(mano));
    }

    return Optional.empty();
  }

  private static int countWins(final List<PlayerId> handWinners, final PlayerId playerId) {

    return (int) handWinners.stream().filter(playerId::equals).count();
  }

}
