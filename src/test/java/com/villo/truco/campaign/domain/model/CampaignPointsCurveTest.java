package com.villo.truco.campaign.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.campaign.domain.model.exceptions.CampaignLadderInvalidCurveInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CampaignPointsCurve")
class CampaignPointsCurveTest {

  private static final int TOTAL_BOTS = 100;
  private static final int TOP_POINTS = 43_200;

  @Test
  @DisplayName("la posición 1 vale exactamente los puntos máximos de la campaña")
  void topPositionIsWorthTopPoints() {

    assertThat(CampaignPointsCurve.pointsForPosition(1, TOTAL_BOTS, TOP_POINTS)).isEqualTo(
        TOP_POINTS);
  }

  @Test
  @DisplayName("los puntos decrecen estrictamente al bajar en el ranking")
  void pointsStrictlyDecreaseDownTheLadder() {

    for (var position = 1; position < TOTAL_BOTS; position++) {
      final var current = CampaignPointsCurve.pointsForPosition(position, TOTAL_BOTS, TOP_POINTS);
      final var below = CampaignPointsCurve.pointsForPosition(position + 1, TOTAL_BOTS, TOP_POINTS);
      assertThat(current).as("posición %d vs %d", position, position + 1).isGreaterThan(below);
    }
  }

  @Test
  @DisplayName("todos los valores quedan redondeados a múltiplos de 5")
  void valuesAreRoundedToMultiplesOfFive() {

    for (var position = 1; position <= TOTAL_BOTS; position++) {
      assertThat(
          CampaignPointsCurve.pointsForPosition(position, TOTAL_BOTS, TOP_POINTS) % 5).isZero();
    }
  }

  @Test
  @DisplayName("la última posición vale pocos puntos para enganchar rápido al jugador nuevo")
  void bottomPositionIsCheap() {

    assertThat(CampaignPointsCurve.pointsForPosition(TOTAL_BOTS, TOTAL_BOTS,
        TOP_POINTS)).isLessThanOrEqualTo(10);
  }

  @Test
  @DisplayName("rechaza posiciones fuera del rango del ranking")
  void rejectsOutOfRangePositions() {

    assertThatThrownBy(
        () -> CampaignPointsCurve.pointsForPosition(0, TOTAL_BOTS, TOP_POINTS)).isInstanceOf(
        CampaignLadderInvalidCurveInputException.class);
    assertThatThrownBy(() -> CampaignPointsCurve.pointsForPosition(TOTAL_BOTS + 1, TOTAL_BOTS,
        TOP_POINTS)).isInstanceOf(CampaignLadderInvalidCurveInputException.class);
  }

}
