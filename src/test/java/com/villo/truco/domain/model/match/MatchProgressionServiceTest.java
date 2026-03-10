package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import org.junit.jupiter.api.Test;

class MatchProgressionServiceTest {

  @Test
  void shouldOnlyUpdateScoreWhenGameIsNotOver() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    final var result = MatchProgressionService.applyPoints(1, 0, 0, 0, playerOne, playerTwo,
        MatchRules.fromGamesToPlay(3), playerOne, 1);

    assertThat(result.scorePlayerOne()).isEqualTo(2);
    assertThat(result.scorePlayerTwo()).isEqualTo(0);
    assertThat(result.gamesWonPlayerOne()).isEqualTo(0);
    assertThat(result.gamesWonPlayerTwo()).isEqualTo(0);
    assertThat(result.gameOver()).isFalse();
    assertThat(result.matchFinished()).isFalse();
    assertThat(result.gameWinner()).isNull();
  }

  @Test
  void shouldResolveGameWhenPlayerReachesThreePoints() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    final var result = MatchProgressionService.applyPoints(2, 1, 0, 0, playerOne, playerTwo,
        MatchRules.fromGamesToPlay(3), playerOne, 1);

    assertThat(result.scorePlayerOne()).isEqualTo(3);
    assertThat(result.scorePlayerTwo()).isEqualTo(1);
    assertThat(result.gamesWonPlayerOne()).isEqualTo(1);
    assertThat(result.gamesWonPlayerTwo()).isEqualTo(0);
    assertThat(result.gameOver()).isTrue();
    assertThat(result.matchFinished()).isFalse();
    assertThat(result.gameWinner()).isEqualTo(playerOne);
  }

  @Test
  void shouldResolveGameToOpponentWhenScoreExceedsThree() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    final var result = MatchProgressionService.applyPoints(2, 2, 0, 0, playerOne, playerTwo,
        MatchRules.fromGamesToPlay(3), playerOne, 2);

    assertThat(result.scorePlayerOne()).isEqualTo(4);
    assertThat(result.scorePlayerTwo()).isEqualTo(2);
    assertThat(result.gamesWonPlayerOne()).isEqualTo(0);
    assertThat(result.gamesWonPlayerTwo()).isEqualTo(1);
    assertThat(result.gameOver()).isTrue();
    assertThat(result.matchFinished()).isFalse();
    assertThat(result.gameWinner()).isEqualTo(playerTwo);
  }

  @Test
  void shouldMarkMatchFinishedWhenGamesToWinReached() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    final var result = MatchProgressionService.applyPoints(2, 0, 1, 0, playerOne, playerTwo,
        MatchRules.fromGamesToPlay(3), playerOne, 1);

    assertThat(result.scorePlayerOne()).isEqualTo(3);
    assertThat(result.scorePlayerTwo()).isEqualTo(0);
    assertThat(result.gamesWonPlayerOne()).isEqualTo(2);
    assertThat(result.gamesWonPlayerTwo()).isEqualTo(0);
    assertThat(result.gameOver()).isTrue();
    assertThat(result.matchFinished()).isTrue();
    assertThat(result.gameWinner()).isEqualTo(playerOne);
  }

}
