package com.villo.truco.domain.model.match.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.match.exceptions.InvalidMatchRulesException;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchRulesTest {

  @Test
  @DisplayName("falla si gamesToWin es menor o igual a cero")
  void failsWhenGamesToWinIsNotPositive() {

    assertThatThrownBy(() -> new MatchRules(0)).isInstanceOf(InvalidMatchRulesException.class)
        .hasMessage("gamesToWin must be greater than zero");
  }

  @Test
  @DisplayName("fromGamesToPlay convierte 1, 3 y 5 al gamesToWin esperado")
  void fromGamesToPlayConvertsToExpectedGamesToWin() {

    assertThat(MatchRules.fromGamesToPlay(GamesToPlay.of(1)).gamesToWin()).isEqualTo(1);
    assertThat(MatchRules.fromGamesToPlay(GamesToPlay.of(3)).gamesToWin()).isEqualTo(2);
    assertThat(MatchRules.fromGamesToPlay(GamesToPlay.of(5)).gamesToWin()).isEqualTo(3);
  }

  @Test
  @DisplayName("GamesToPlay falla para valores fuera de 1, 3 o 5")
  void gamesToPlayFailsForInvalidValues() {

    assertThatThrownBy(() -> GamesToPlay.of(2)).isInstanceOf(Exception.class);
  }

}
