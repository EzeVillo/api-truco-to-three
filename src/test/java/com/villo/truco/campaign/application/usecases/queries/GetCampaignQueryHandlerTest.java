package com.villo.truco.campaign.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.campaign.application.dto.CampaignRankingEntryDTO;
import com.villo.truco.campaign.application.support.FixedCampaignLadderProvider;
import com.villo.truco.campaign.application.support.InMemoryCampaignProgressRepository;
import com.villo.truco.campaign.domain.model.CampaignBot;
import com.villo.truco.campaign.domain.model.CampaignLadder;
import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetCampaignQueryHandler")
class GetCampaignQueryHandlerTest {

  private final CampaignBot top = new CampaignBot(PlayerId.generate(), "Cacho Medina", 1, 1000);
  private final CampaignBot middle = new CampaignBot(PlayerId.generate(), "Tito Toledo", 2, 500);
  private final CampaignBot bottom = new CampaignBot(PlayerId.generate(), "Rulo Suárez", 3, 100);
  private final CampaignLadder ladder = new CampaignLadder(List.of(top, middle, bottom));
  private final GetCampaignQueryHandler handler = new GetCampaignQueryHandler(progressRepository,
      new FixedCampaignLadderProvider(ladder));
  private final InMemoryCampaignProgressRepository progressRepository = new InMemoryCampaignProgressRepository();

  @Test
  @DisplayName("el jugador nuevo arranca último y solo puede desafiar al bot del fondo")
  void newPlayerStartsLastAndCanChallengeBottomBot() {

    final var playerId = PlayerId.generate();

    final var dto = handler.handle(new GetCampaignQuery(playerId.value().toString()));

    assertThat(dto.playerPosition()).isEqualTo(4);
    assertThat(dto.playerPoints()).isZero();
    assertThat(dto.totalBots()).isEqualTo(3);
    assertThat(dto.topOneReached()).isFalse();
    assertThat(dto.pointsToNextPosition()).isEqualTo(101);
    assertThat(dto.ranking()).hasSize(4);
    assertThat(dto.ranking().getLast().player()).isTrue();

    final var challengeable = dto.ranking().stream().filter(CampaignRankingEntryDTO::challengeable)
        .map(CampaignRankingEntryDTO::participantId).toList();
    assertThat(challengeable).containsExactly(bottom.playerId().value().toString());
  }

  @Test
  @DisplayName("el jugador aparece intercalado en su posición real del ranking")
  void playerIsInsertedAtItsActualPosition() {

    final var playerId = PlayerId.generate();
    final var progress = CampaignProgress.create(playerId);
    final var matchId = MatchId.generate();
    progress.startChallenge(bottom, matchId, ladder);
    progress.resolveChallengeWon(matchId, 3, 0, ladder);
    progress.clearDomainEvents();
    progressRepository.save(progress);

    final var dto = handler.handle(new GetCampaignQuery(playerId.value().toString()));

    assertThat(dto.playerPoints()).isEqualTo(300);
    assertThat(dto.playerPosition()).isEqualTo(3);
    assertThat(dto.defeatedRivals()).isEqualTo(1);
    assertThat(dto.ranking().get(2).player()).isTrue();
  }

  @Test
  @DisplayName("tras alcanzar el top 1 todos los bots quedan desafiables")
  void afterTopOneEveryBotIsChallengeable() {

    final var playerId = PlayerId.generate();
    final var progress = CampaignProgress.create(playerId);
    beat(progress, bottom);
    beat(progress, middle);
    beat(progress, top);
    beat(progress, top);
    progress.clearDomainEvents();
    progressRepository.save(progress);

    final var dto = handler.handle(new GetCampaignQuery(playerId.value().toString()));

    assertThat(dto.topOneReached()).isTrue();
    assertThat(dto.allRivalsDefeated()).isTrue();
    assertThat(dto.playerPosition()).isEqualTo(1);
    assertThat(dto.pointsToNextPosition()).isNull();
    assertThat(dto.ranking()).filteredOn(CampaignRankingEntryDTO::challengeable).hasSize(3);
  }

  private void beat(final CampaignProgress progress, final CampaignBot rival) {

    final var matchId = MatchId.generate();
    progress.startChallenge(rival, matchId, ladder);
    progress.resolveChallengeWon(matchId, 3, 0, ladder);
  }

}
