package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoundTerminationPolicyTest {

  private final PlayerId playerOne = PlayerId.generate();
  private final PlayerId playerTwo = PlayerId.generate();
  private final PlayerId mano = playerOne;

  @Test
  @DisplayName("sin manos jugadas no hay ganador")
  void noHandsPlayed() {

    assertThat(resolve(List.of())).isEmpty();
  }

  @Test
  @DisplayName("gana primera + empata segunda → gana quien ganó primera")
  void winsFirstTiesSecond() {

    assertThat(resolve(Arrays.asList(playerOne, null))).contains(playerOne);
  }

  @Test
  @DisplayName("empata primera + gana segunda → gana quien ganó segunda")
  void tiesFirstWinsSecond() {

    assertThat(resolve(Arrays.asList(null, playerTwo))).contains(playerTwo);
  }

  @Test
  @DisplayName("gana las dos primeras → gana esa ronda inmediatamente")
  void winsBothFirstAndSecond() {

    assertThat(resolve(List.of(playerOne, playerOne))).contains(playerOne);
  }

  @Test
  @DisplayName("1-1 tras dos manos → sin ganador todavía")
  void splitAfterTwoHands() {

    assertThat(resolve(List.of(playerOne, playerTwo))).isEmpty();
  }

  @Test
  @DisplayName("dos pardas → sin ganador todavía")
  void twoTies() {

    assertThat(resolve(Arrays.asList(null, null))).isEmpty();
  }

  @Test
  @DisplayName("tres pardas → gana mano")
  void allThreeTies() {

    assertThat(resolve(Arrays.asList(null, null, null))).contains(mano);
  }

  @Test
  @DisplayName("1-1 con parda en tercera → gana quien ganó la primera")
  void splitThenTieInThird() {

    assertThat(resolve(Arrays.asList(playerOne, playerTwo, null))).contains(playerOne);
  }

  @Test
  @DisplayName("parda-parda y gana en tercera → gana la tercera")
  void twoTiesThenWinnerInThird() {

    assertThat(resolve(Arrays.asList(null, null, playerTwo))).contains(playerTwo);
  }

  @Test
  @DisplayName("gana primera + parda segunda + parda tercera → gana primera (teórico, no alcanzable en producción)")
  void winsFirstTiesThenThird() {

    assertThat(resolve(Arrays.asList(playerOne, null, null))).contains(playerOne);
  }

  private java.util.Optional<PlayerId> resolve(final List<PlayerId> winners) {

    return RoundTerminationPolicy.resolveWinner(winners, playerOne, playerTwo, mano);
  }

}
