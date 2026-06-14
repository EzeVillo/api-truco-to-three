package com.villo.truco.campaign.application.eventhandlers;

import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.campaign.application.events.CampaignEventNotification;
import com.villo.truco.campaign.application.ports.out.CampaignDomainEventHandler;
import com.villo.truco.campaign.domain.model.events.CampaignBotUnlockedForCasualEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeLostEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeWonEvent;
import com.villo.truco.campaign.domain.model.events.CampaignDomainEvent;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CampaignNotificationEventTranslator implements
    CampaignDomainEventHandler<CampaignDomainEvent> {

  private static final String MATCH_POINTS_EVENT_TYPE = "CAMPAIGN_MATCH_POINTS";
  private static final String BOT_UNLOCKED_EVENT_TYPE = "CAMPAIGN_BOT_UNLOCKED";

  private final ApplicationEventPublisher applicationEventPublisher;

  public CampaignNotificationEventTranslator(
      final ApplicationEventPublisher applicationEventPublisher) {

    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
  }

  @Override
  public Class<CampaignDomainEvent> eventType() {

    return CampaignDomainEvent.class;
  }

  @Override
  public void handle(final CampaignDomainEvent event) {

    switch (event) {
      case CampaignChallengeWonEvent e ->
          this.publish(e.getPlayerId(), e.getTimestamp(), e.getRivalId(), e.getMatchId(), true,
              e.getPointsAwarded(), e.getTotalPoints(), e.getPreviousPosition(),
              e.getNewPosition());
      case CampaignChallengeLostEvent e ->
          this.publish(e.getPlayerId(), e.getTimestamp(), e.getRivalId(), e.getMatchId(), false, 0,
              e.getTotalPoints(), e.getPreviousPosition(), e.getNewPosition());
      case CampaignBotUnlockedForCasualEvent e ->
          this.publishBotUnlocked(e.getPlayerId(), e.getTimestamp(), e.getRivalId(),
              e.getMatchId());
      default -> {
      }
    }
  }

  private void publishBotUnlocked(final PlayerId playerId, final long timestamp,
      final PlayerId rivalId, final MatchId matchId) {

    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("botId", rivalId.value().toString());
    payload.put("matchId", matchId.value().toString());

    this.applicationEventPublisher.publish(
        new CampaignEventNotification(List.of(playerId), BOT_UNLOCKED_EVENT_TYPE, timestamp,
            payload));
  }

  private void publish(final PlayerId playerId, final long timestamp, final PlayerId rivalId,
      final MatchId matchId, final boolean won, final int pointsAwarded, final int totalPoints,
      final int previousPosition, final int newPosition) {

    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("matchId", matchId.value().toString());
    payload.put("rivalId", rivalId.value().toString());
    payload.put("won", won);
    payload.put("pointsAwarded", pointsAwarded);
    payload.put("totalPoints", totalPoints);
    payload.put("previousPosition", previousPosition);
    payload.put("newPosition", newPosition);

    this.applicationEventPublisher.publish(
        new CampaignEventNotification(List.of(playerId), MATCH_POINTS_EVENT_TYPE, timestamp,
            payload));
  }

}
