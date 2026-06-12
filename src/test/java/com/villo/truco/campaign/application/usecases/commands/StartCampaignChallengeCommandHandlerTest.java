package com.villo.truco.campaign.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.CreateBotMatchCommand;
import com.villo.truco.application.dto.CreateBotMatchDTO;
import com.villo.truco.application.exceptions.BotNotFoundException;
import com.villo.truco.application.ports.in.CreateBotMatchUseCase;
import com.villo.truco.campaign.application.exceptions.CampaignRivalSelectionRequiredException;
import com.villo.truco.campaign.application.support.FixedCampaignLadderProvider;
import com.villo.truco.campaign.application.support.InMemoryCampaignMatchRegistry;
import com.villo.truco.campaign.application.support.InMemoryCampaignProgressRepository;
import com.villo.truco.campaign.application.support.RecordingCampaignEventNotifier;
import com.villo.truco.campaign.domain.model.CampaignBot;
import com.villo.truco.campaign.domain.model.CampaignLadder;
import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeStartedEvent;
import com.villo.truco.campaign.domain.model.exceptions.BotNotImmediatelyAboveException;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StartCampaignChallengeCommandHandler")
class StartCampaignChallengeCommandHandlerTest {

  private final CampaignBot top = new CampaignBot(PlayerId.generate(), "Cacho Medina", 1, 1000);
  private final CampaignBot middle = new CampaignBot(PlayerId.generate(), "Tito Toledo", 2, 500);
  private final CampaignBot bottom = new CampaignBot(PlayerId.generate(), "Rulo Suárez", 3, 100);
  private final CampaignLadder ladder = new CampaignLadder(List.of(top, middle, bottom));

  private final InMemoryCampaignProgressRepository progressRepository = new InMemoryCampaignProgressRepository();
  private final InMemoryCampaignMatchRegistry matchRegistry = new InMemoryCampaignMatchRegistry();
  private final RecordingCampaignEventNotifier eventNotifier = new RecordingCampaignEventNotifier();
  private final MatchId createdMatchId = MatchId.generate();

  private final CreateBotMatchUseCase createBotMatch = new RecordingCreateBotMatch(createdMatchId);

  private final StartCampaignChallengeCommandHandler handler = new StartCampaignChallengeCommandHandler(
      progressRepository, new FixedCampaignLadderProvider(ladder), matchRegistry, eventNotifier,
      createBotMatch);

  @Test
  @DisplayName("sin botId desafía al bot inmediatamente superior y registra el match de campaña")
  void challengesNextRivalWhenNoBotIdProvided() {

    final var playerId = PlayerId.generate();

    final var dto = handler.handle(
        new StartCampaignChallengeCommand(playerId.value().toString(), null));

    assertThat(dto.matchId()).isEqualTo(createdMatchId.value().toString());
    assertThat(dto.rivalId()).isEqualTo(bottom.playerId().value().toString());
    assertThat(dto.rivalPosition()).isEqualTo(3);
    assertThat(matchRegistry.isCampaignMatch(createdMatchId)).isTrue();
    assertThat(eventNotifier.published()).singleElement()
        .isInstanceOf(CampaignChallengeStartedEvent.class);
    assertThat(progressRepository.findByPlayerId(playerId)).isPresent();
  }

  @Test
  @DisplayName("desafiar a un bot que no es el inmediato superior es rechazado antes del top 1")
  void rejectsChallengeToNonAdjacentBotBeforeTopOne() {

    final var playerId = PlayerId.generate();

    assertThatThrownBy(() -> handler.handle(
        new StartCampaignChallengeCommand(playerId.value().toString(),
            top.playerId().value().toString()))).isInstanceOf(
        BotNotImmediatelyAboveException.class);

    assertThat(matchRegistry.isCampaignMatch(createdMatchId)).isFalse();
  }

  @Test
  @DisplayName("un botId desconocido produce BotNotFoundException")
  void unknownBotIdFails() {

    final var playerId = PlayerId.generate();

    assertThatThrownBy(() -> handler.handle(
        new StartCampaignChallengeCommand(playerId.value().toString(),
            PlayerId.generate().value().toString()))).isInstanceOf(BotNotFoundException.class);
  }

  @Test
  @DisplayName("sin botId tras alcanzar el top 1 ya no hay rival inmediato y se exige seleccionar uno")
  void requiresRivalSelectionAtTopOne() {

    final var playerId = PlayerId.generate();
    final var progress = CampaignProgress.create(playerId);
    final var seedMatch = MatchId.generate();
    progress.startChallenge(bottom, seedMatch, ladder);
    progress.resolveChallengeWon(seedMatch, 3, 0, ladder);
    final var seedMatch2 = MatchId.generate();
    progress.startChallenge(middle, seedMatch2, ladder);
    progress.resolveChallengeWon(seedMatch2, 3, 0, ladder);
    final var seedMatch3 = MatchId.generate();
    progress.startChallenge(top, seedMatch3, ladder);
    progress.resolveChallengeWon(seedMatch3, 3, 0, ladder);
    final var seedMatch4 = MatchId.generate();
    progress.startChallenge(top, seedMatch4, ladder);
    progress.resolveChallengeWon(seedMatch4, 3, 0, ladder);
    progress.clearDomainEvents();
    progressRepository.save(progress);

    assertThat(progress.isTopOneReached()).isTrue();

    assertThatThrownBy(() -> handler.handle(
        new StartCampaignChallengeCommand(playerId.value().toString(), null))).isInstanceOf(
        CampaignRivalSelectionRequiredException.class);
  }

  private record RecordingCreateBotMatch(MatchId matchId) implements CreateBotMatchUseCase {

    @Override
    public CreateBotMatchDTO handle(final CreateBotMatchCommand command) {

      return new CreateBotMatchDTO(matchId.value().toString());
    }

  }

}
