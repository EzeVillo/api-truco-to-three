package com.villo.truco.campaign.application.services;

import com.villo.truco.campaign.domain.ports.CampaignEventNotifier;
import com.villo.truco.campaign.domain.ports.CampaignLadderProvider;
import com.villo.truco.campaign.domain.ports.CampaignMatchRegistry;
import com.villo.truco.campaign.domain.ports.CampaignProgressRepository;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CampaignChallengeResolutionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      CampaignChallengeResolutionService.class);

  private final CampaignProgressRepository campaignProgressRepository;
  private final CampaignLadderProvider campaignLadderProvider;
  private final CampaignMatchRegistry campaignMatchRegistry;
  private final CampaignEventNotifier campaignEventNotifier;

  public CampaignChallengeResolutionService(
      final CampaignProgressRepository campaignProgressRepository,
      final CampaignLadderProvider campaignLadderProvider,
      final CampaignMatchRegistry campaignMatchRegistry,
      final CampaignEventNotifier campaignEventNotifier) {

    this.campaignProgressRepository = Objects.requireNonNull(campaignProgressRepository);
    this.campaignLadderProvider = Objects.requireNonNull(campaignLadderProvider);
    this.campaignMatchRegistry = Objects.requireNonNull(campaignMatchRegistry);
    this.campaignEventNotifier = Objects.requireNonNull(campaignEventNotifier);
  }

  private static MatchOutcomeView resolveOutcome(final MatchDomainEvent event) {

    return switch (event) {
      case MatchFinishedEvent e -> new MatchOutcomeView(e.getWinnerSeat(), e.getGamesWonPlayerOne(),
          e.getGamesWonPlayerTwo());
      case MatchForfeitedEvent e ->
          new MatchOutcomeView(e.getWinnerSeat(), e.getGamesWonPlayerOne(),
              e.getGamesWonPlayerTwo());
      case MatchAbandonedEvent e ->
          new MatchOutcomeView(e.getWinnerSeat(), e.getGamesWonPlayerOne(),
              e.getGamesWonPlayerTwo());
      default -> null;
    };
  }

  public void handle(final MatchDomainEvent event) {

    final var outcome = resolveOutcome(event);
    if (outcome == null) {
      return;
    }

    final var matchId = event.getMatchId();
    final var playerId = this.campaignMatchRegistry.findPlayerByMatchId(matchId).orElse(null);
    if (playerId == null) {
      return;
    }

    final var progress = this.campaignProgressRepository.findByPlayerId(playerId).orElse(null);
    if (progress == null || !progress.isActiveChallenge(matchId)) {
      LOGGER.debug("Match {} is not the active campaign challenge of player {}, skipping", matchId,
          playerId);
      return;
    }

    final var playerSeat =
        playerId.equals(event.getPlayerOne()) ? PlayerSeat.PLAYER_ONE : PlayerSeat.PLAYER_TWO;

    if (outcome.playerWon(playerSeat)) {
      progress.resolveChallengeWon(matchId, outcome.gamesWonBy(playerSeat),
          outcome.gamesWonAgainst(playerSeat), this.campaignLadderProvider.ladder());
    } else {
      progress.resolveChallengeLost(matchId);
    }

    this.campaignProgressRepository.save(progress);
    this.campaignEventNotifier.publishDomainEvents(progress.getCampaignDomainEvents());
    progress.clearDomainEvents();

    LOGGER.info("Campaign challenge resolved: playerId={}, matchId={}, playerWon={}", playerId,
        matchId, outcome.winnerSeat() == playerSeat);
  }

  private record MatchOutcomeView(PlayerSeat winnerSeat, int gamesWonPlayerOne,
                                  int gamesWonPlayerTwo) {

    boolean playerWon(final PlayerSeat playerSeat) {

      return this.winnerSeat == playerSeat;
    }

    int gamesWonBy(final PlayerSeat seat) {

      return seat == PlayerSeat.PLAYER_ONE ? this.gamesWonPlayerOne : this.gamesWonPlayerTwo;
    }

    int gamesWonAgainst(final PlayerSeat seat) {

      return seat == PlayerSeat.PLAYER_ONE ? this.gamesWonPlayerTwo : this.gamesWonPlayerOne;
    }

  }

}
