package com.villo.truco.domain.model.cup;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.cup.valueobjects.BoutStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cup.isPlayerStillCompeting")
class CupPlayerAvailabilityTest {

  private static Cup createStartedCup(final PlayerId... players) {

    final var cup = Cup.create(players[0], players.length, GamesToPlay.of(3), Visibility.PRIVATE);
    for (int i = 1; i < players.length; i++) {
      cup.join(players[i], cup.getInviteCode());
    }
    cup.start(players[0]);
    return cup;
  }

  @Test
  @DisplayName("jugador activo sin bouts FINISHED → true")
  void returnsTrueForActivePlayer() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = createStartedCup(p1, p2, p3, p4);

    for (final var participant : new PlayerId[]{p1, p2, p3, p4}) {
      assertThat(cup.isPlayerStillCompeting(participant)).isTrue();
    }
  }

  @Test
  @DisplayName("jugador forfeitado → false")
  void returnsFalseForForfeitedPlayer() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = createStartedCup(p1, p2, p3, p4);

    cup.forfeitPlayer(p1);

    assertThat(cup.isPlayerStillCompeting(p1)).isFalse();
  }

  @Test
  @DisplayName("jugador eliminado (perdió un bout FINISHED) → false")
  void returnsFalseForEliminatedPlayer() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = createStartedCup(p1, p2, p3, p4);

    final var pendingBout = cup.getBouts().stream().filter(b -> b.status() == BoutStatus.PENDING)
        .findFirst().orElseThrow();

    final var matchId = MatchId.generate();
    cup.linkBoutMatch(pendingBout.boutId(), matchId);
    cup.recordMatchWinner(matchId, pendingBout.playerOne());

    assertThat(cup.isPlayerStillCompeting(pendingBout.playerTwo())).isFalse();
    assertThat(cup.isPlayerStillCompeting(pendingBout.playerOne())).isTrue();
  }

  @Test
  @DisplayName("jugador que no es participante → true (no está en ningún bout perdido)")
  void returnsTrueForNonParticipant() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var outsider = PlayerId.generate();
    final var cup = createStartedCup(p1, p2, p3, p4);

    assertThat(cup.isPlayerStillCompeting(outsider)).isTrue();
  }

}
