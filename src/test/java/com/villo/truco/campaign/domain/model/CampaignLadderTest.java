package com.villo.truco.campaign.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.campaign.domain.model.exceptions.CampaignLadderEmptyException;
import com.villo.truco.campaign.domain.model.exceptions.CampaignLadderPointsNotStrictlyDecreasingException;
import com.villo.truco.campaign.domain.model.exceptions.CampaignLadderPositionsNotContiguousException;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignPoints;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CampaignLadder")
class CampaignLadderTest {

  private final CampaignBot top = new CampaignBot(PlayerId.generate(), "Cacho Medina", 1, 1000);
  private final CampaignBot middle = new CampaignBot(PlayerId.generate(), "Tito Toledo", 2, 500);
  private final CampaignBot bottom = new CampaignBot(PlayerId.generate(), "Rulo Suárez", 3, 100);
  private final CampaignLadder ladder = new CampaignLadder(List.of(top, middle, bottom));

  @Test
  @DisplayName("el jugador sin puntos queda último, debajo de todos los bots")
  void playerWithoutPointsIsLast() {

    assertThat(ladder.positionFor(CampaignPoints.ZERO)).isEqualTo(4);
  }

  @Test
  @DisplayName("empatar en puntos con un bot no lo supera: se exige estrictamente más")
  void tieDoesNotSurpassBot() {

    assertThat(ladder.positionFor(new CampaignPoints(100))).isEqualTo(4);
    assertThat(ladder.positionFor(new CampaignPoints(101))).isEqualTo(3);
  }

  @Test
  @DisplayName("nextRival devuelve el bot inmediatamente superior al jugador")
  void nextRivalIsTheBotImmediatelyAbove() {

    assertThat(ladder.nextRival(CampaignPoints.ZERO)).contains(bottom);
    assertThat(ladder.nextRival(new CampaignPoints(101))).contains(middle);
    assertThat(ladder.nextRival(new CampaignPoints(501))).contains(top);
  }

  @Test
  @DisplayName("nextRival es vacío cuando el jugador supera a todos los bots")
  void nextRivalIsEmptyAtTheTop() {

    assertThat(ladder.nextRival(new CampaignPoints(1001))).isEmpty();
  }

  @Test
  @DisplayName("pointsToOvertakeNextRival devuelve los puntos para superar al rival inmediato")
  void pointsToOvertakeNextRivalReturnsThresholdGap() {

    assertThat(ladder.pointsToOvertakeNextRival(CampaignPoints.ZERO)).hasValue(101);
    assertThat(ladder.pointsToOvertakeNextRival(new CampaignPoints(101))).hasValue(400);
  }

  @Test
  @DisplayName("pointsToOvertakeNextRival es vacío cuando el jugador ya es top 1")
  void pointsToOvertakeNextRivalIsEmptyAtTheTop() {

    assertThat(ladder.pointsToOvertakeNextRival(new CampaignPoints(1001))).isEmpty();
  }

  @Test
  @DisplayName("rechaza posiciones no contiguas")
  void rejectsNonContiguousPositions() {

    final var misplaced = new CampaignBot(PlayerId.generate(), "Beto Quiroga", 3, 500);

    assertThatThrownBy(() -> new CampaignLadder(List.of(top, misplaced))).isInstanceOf(
        CampaignLadderPositionsNotContiguousException.class);
  }

  @Test
  @DisplayName("rechaza puntos que no decrecen estrictamente con la posición")
  void rejectsNonDecreasingPoints() {

    final var tied = new CampaignBot(PlayerId.generate(), "Beto Quiroga", 2, 1000);

    assertThatThrownBy(() -> new CampaignLadder(List.of(top, tied))).isInstanceOf(
        CampaignLadderPointsNotStrictlyDecreasingException.class);
  }

  @Test
  @DisplayName("rechaza una escalera vacía")
  void rejectsEmptyLadder() {

    assertThatThrownBy(() -> new CampaignLadder(List.of())).isInstanceOf(
        CampaignLadderEmptyException.class);
  }

}
