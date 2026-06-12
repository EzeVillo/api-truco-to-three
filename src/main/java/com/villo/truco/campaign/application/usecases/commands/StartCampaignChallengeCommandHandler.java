package com.villo.truco.campaign.application.usecases.commands;

import com.villo.truco.application.commands.CreateBotMatchCommand;
import com.villo.truco.application.exceptions.BotNotFoundException;
import com.villo.truco.application.ports.in.CreateBotMatchUseCase;
import com.villo.truco.campaign.application.dto.StartCampaignChallengeDTO;
import com.villo.truco.campaign.application.exceptions.CampaignRivalSelectionRequiredException;
import com.villo.truco.campaign.application.services.CampaignUserGuard;
import com.villo.truco.campaign.domain.model.CampaignBot;
import com.villo.truco.campaign.domain.model.CampaignLadder;
import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.campaign.domain.ports.CampaignEventNotifier;
import com.villo.truco.campaign.domain.ports.CampaignLadderProvider;
import com.villo.truco.campaign.domain.ports.CampaignMatchRegistry;
import com.villo.truco.campaign.domain.ports.CampaignProgressRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.Objects;

public final class StartCampaignChallengeCommandHandler implements StartCampaignChallengeUseCase {

  private static final GamesToPlay CAMPAIGN_GAMES_TO_PLAY = GamesToPlay.of(5);

  private final CampaignProgressRepository campaignProgressRepository;
  private final CampaignLadderProvider campaignLadderProvider;
  private final CampaignMatchRegistry campaignMatchRegistry;
  private final CampaignEventNotifier campaignEventNotifier;
  private final CreateBotMatchUseCase createBotMatch;
  private final CampaignUserGuard campaignUserGuard;

  public StartCampaignChallengeCommandHandler(
      final CampaignProgressRepository campaignProgressRepository,
      final CampaignLadderProvider campaignLadderProvider,
      final CampaignMatchRegistry campaignMatchRegistry,
      final CampaignEventNotifier campaignEventNotifier, final CreateBotMatchUseCase createBotMatch,
      final CampaignUserGuard campaignUserGuard) {

    this.campaignProgressRepository = Objects.requireNonNull(campaignProgressRepository);
    this.campaignLadderProvider = Objects.requireNonNull(campaignLadderProvider);
    this.campaignMatchRegistry = Objects.requireNonNull(campaignMatchRegistry);
    this.campaignEventNotifier = Objects.requireNonNull(campaignEventNotifier);
    this.createBotMatch = Objects.requireNonNull(createBotMatch);
    this.campaignUserGuard = Objects.requireNonNull(campaignUserGuard);
  }

  @Override
  public StartCampaignChallengeDTO handle(final StartCampaignChallengeCommand command) {

    this.campaignUserGuard.ensureRegisteredUser(command.playerId());

    final var ladder = this.campaignLadderProvider.ladder();
    final var progress = this.campaignProgressRepository.findByPlayerId(command.playerId())
        .orElseGet(() -> CampaignProgress.create(command.playerId()));

    final var rival = this.resolveRival(command, progress, ladder);
    progress.ensureCanChallenge(rival, ladder);

    final var createBotMatchDTO = this.createBotMatch.handle(
        new CreateBotMatchCommand(command.playerId(), CAMPAIGN_GAMES_TO_PLAY, rival.playerId()));
    final var matchId = MatchId.of(createBotMatchDTO.matchId());

    progress.startChallenge(rival, matchId, ladder);
    this.campaignMatchRegistry.register(matchId, command.playerId());
    this.campaignProgressRepository.save(progress);
    this.campaignEventNotifier.publishDomainEvents(progress.getCampaignDomainEvents());
    progress.clearDomainEvents();

    return new StartCampaignChallengeDTO(createBotMatchDTO.matchId(),
        rival.playerId().value().toString(), rival.displayName(), rival.position());
  }

  private CampaignBot resolveRival(final StartCampaignChallengeCommand command,
      final CampaignProgress progress, final CampaignLadder ladder) {

    if (command.botId() != null) {
      return ladder.findBot(command.botId())
          .orElseThrow(() -> new BotNotFoundException(command.botId()));
    }

    return ladder.nextRival(progress.getPoints())
        .orElseThrow(CampaignRivalSelectionRequiredException::new);
  }

}
