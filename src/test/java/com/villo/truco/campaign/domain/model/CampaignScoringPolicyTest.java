package com.villo.truco.campaign.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.campaign.domain.model.exceptions.InvalidCampaignVictoryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CampaignScoringPolicy")
class CampaignScoringPolicyTest {

  @Test
  @DisplayName("otorga 100 puntos por cada game de diferencia: 3-0 da 300, 3-1 da 200, 3-2 da 100")
  void awardsHundredPointsPerGameOfMargin() {

    assertThat(CampaignScoringPolicy.pointsForVictory(3, 0)).isEqualTo(300);
    assertThat(CampaignScoringPolicy.pointsForVictory(3, 1)).isEqualTo(200);
    assertThat(CampaignScoringPolicy.pointsForVictory(3, 2)).isEqualTo(100);
  }

  @Test
  @DisplayName("una victoria sin margen de games otorga el mínimo de 100 puntos")
  void victoryWithoutMarginAwardsMinimum() {

    assertThat(CampaignScoringPolicy.pointsForVictory(0, 0)).isEqualTo(100);
  }

  @Test
  @DisplayName("rechaza cantidades negativas de games")
  void rejectsNegativeGames() {

    assertThatThrownBy(() -> CampaignScoringPolicy.pointsForVictory(-1, 0)).isInstanceOf(
        InvalidCampaignVictoryException.class);
  }

}
