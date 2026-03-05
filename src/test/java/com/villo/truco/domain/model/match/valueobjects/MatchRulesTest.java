package com.villo.truco.domain.model.match.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.match.exceptions.InvalidMatchRulesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchRulesTest {

  @Test
  @DisplayName("defaultRules usa 3 juegos y 3 puntos por juego")
  void defaultRulesAreThreeAndThree() {

    final var rules = MatchRules.defaultRules();

    assertThat(rules.gamesToWin()).isEqualTo(3);
    assertThat(rules.pointsToWinGame()).isEqualTo(3);
  }

  @Test
  @DisplayName("falla si gamesToWin es menor o igual a cero")
  void failsWhenGamesToWinIsNotPositive() {

    assertThatThrownBy(() -> new MatchRules(0, 3)).isInstanceOf(InvalidMatchRulesException.class)
        .hasMessage("gamesToWin must be greater than zero");
  }

  @Test
  @DisplayName("falla si pointsToWinGame es menor o igual a cero")
  void failsWhenPointsToWinGameIsNotPositive() {

    assertThatThrownBy(() -> new MatchRules(3, 0)).isInstanceOf(InvalidMatchRulesException.class)
        .hasMessage("pointsToWinGame must be greater than zero");
  }

}
