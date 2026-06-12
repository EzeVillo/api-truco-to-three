package com.villo.truco.campaign.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.campaign.application.support.FixedCampaignLadderProvider;
import com.villo.truco.campaign.application.support.InMemoryCampaignMatchRegistry;
import com.villo.truco.campaign.application.support.InMemoryCampaignProgressRepository;
import com.villo.truco.campaign.application.support.RecordingCampaignEventNotifier;
import com.villo.truco.campaign.domain.model.CampaignBot;
import com.villo.truco.campaign.domain.model.CampaignLadder;
import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeLostEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeWonEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CampaignChallengeResolutionService")
class CampaignChallengeResolutionServiceTest {

  private final CampaignBot top = new CampaignBot(PlayerId.generate(), "Cacho Medina", 1, 1000);
  private final CampaignBot bottom = new CampaignBot(PlayerId.generate(), "Rulo Suárez", 2, 100);
  private final CampaignLadder ladder = new CampaignLadder(List.of(top, bottom));
  private final InMemoryCampaignProgressRepository progressRepository = new InMemoryCampaignProgressRepository();
  private final InMemoryCampaignMatchRegistry matchRegistry = new InMemoryCampaignMatchRegistry();
  private final RecordingCampaignEventNotifier eventNotifier = new RecordingCampaignEventNotifier();
  private final CampaignChallengeResolutionService service;

  CampaignChallengeResolutionServiceTest() {

    this.service = new CampaignChallengeResolutionService(progressRepository,
        new FixedCampaignLadderProvider(ladder), matchRegistry, eventNotifier);
  }

  @Test
  @DisplayName("acredita puntos cuando el jugador (player one) gana su desafío de campaña")
  void awardsPointsWhenCampaignPlayerWinsAsPlayerOne() {

    final var playerId = PlayerId.generate();
    final var matchId = seedActiveChallenge(playerId, bottom);

    service.handle(
        new MatchFinishedEvent(matchId, playerId, bottom.playerId(), PlayerSeat.PLAYER_ONE, 3, 1));

    assertThat(
        progressRepository.findByPlayerId(playerId).orElseThrow().getPoints().value()).isEqualTo(
        200);
    assertThat(eventNotifier.published()).anySatisfy(
        event -> assertThat(event).isInstanceOf(CampaignChallengeWonEvent.class));
  }

  @Test
  @DisplayName("interpreta correctamente los games cuando el jugador es player two")
  void mapsGamesWhenCampaignPlayerIsPlayerTwo() {

    final var playerId = PlayerId.generate();
    final var matchId = seedActiveChallenge(playerId, bottom);

    service.handle(
        new MatchFinishedEvent(matchId, bottom.playerId(), playerId, PlayerSeat.PLAYER_TWO, 0, 3));

    assertThat(
        progressRepository.findByPlayerId(playerId).orElseThrow().getPoints().value()).isEqualTo(
        300);
  }

  @Test
  @DisplayName("una derrota no descuenta puntos y registra el head-to-head")
  void lossKeepsPointsAndRecordsHeadToHead() {

    final var playerId = PlayerId.generate();
    final var matchId = seedActiveChallenge(playerId, bottom);

    service.handle(
        new MatchFinishedEvent(matchId, playerId, bottom.playerId(), PlayerSeat.PLAYER_TWO, 3, 1));

    final var progress = progressRepository.findByPlayerId(playerId).orElseThrow();
    assertThat(progress.getPoints().value()).isZero();
    assertThat(progress.getRivalRecords().get(bottom.playerId()).losses()).isEqualTo(1);
    assertThat(eventNotifier.published()).anySatisfy(
        event -> assertThat(event).isInstanceOf(CampaignChallengeLostEvent.class));
  }

  @Test
  @DisplayName("ignora matches que no pertenecen a la campaña")
  void ignoresNonCampaignMatches() {

    final var playerId = PlayerId.generate();

    service.handle(new MatchFinishedEvent(MatchId.generate(), playerId, bottom.playerId(),
        PlayerSeat.PLAYER_ONE, 3, 0));

    assertThat(eventNotifier.published()).isEmpty();
    assertThat(progressRepository.findByPlayerId(playerId)).isEmpty();
  }

  private MatchId seedActiveChallenge(final PlayerId playerId, final CampaignBot rival) {

    final var matchId = MatchId.generate();
    final var progress = CampaignProgress.create(playerId);
    progress.startChallenge(rival, matchId, ladder);
    progress.clearDomainEvents();
    progressRepository.save(progress);
    matchRegistry.register(matchId, playerId);
    return matchId;
  }

}
