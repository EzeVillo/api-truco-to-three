package com.villo.truco.campaign.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.campaign.application.events.CampaignEventNotification;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeLostEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeWonEvent;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CampaignNotificationEventTranslator")
class CampaignNotificationEventTranslatorTest {

  private final PlayerId playerId = PlayerId.generate();
  private final PlayerId rivalId = PlayerId.generate();
  private final MatchId matchId = MatchId.generate();

  @Test
  @DisplayName("una victoria emite CAMPAIGN_MATCH_POINTS con los puntos ganados y el salto de posición")
  void winPublishesPointsNotification() {

    final var publisher = new RecordingPublisher();
    final var translator = new CampaignNotificationEventTranslator(publisher);

    translator.handle(new CampaignChallengeWonEvent(playerId, rivalId, matchId, 300, 300, 4, 1));

    assertThat(publisher.events()).singleElement()
        .isInstanceOfSatisfying(CampaignEventNotification.class, notification -> {
          assertThat(notification.recipients()).containsExactly(playerId);
          assertThat(notification.eventType()).isEqualTo("CAMPAIGN_MATCH_POINTS");
          assertThat(notification.payload()).containsEntry("won", true)
              .containsEntry("pointsAwarded", 300).containsEntry("totalPoints", 300)
              .containsEntry("previousPosition", 4).containsEntry("newPosition", 1)
              .containsEntry("rivalId", rivalId.value().toString())
              .containsEntry("matchId", matchId.value().toString());
        });
  }

  @Test
  @DisplayName("una derrota emite CAMPAIGN_MATCH_POINTS con 0 puntos y sin cambio de posición")
  void lossPublishesZeroPointsNotification() {

    final var publisher = new RecordingPublisher();
    final var translator = new CampaignNotificationEventTranslator(publisher);

    translator.handle(new CampaignChallengeLostEvent(playerId, rivalId, matchId, 100, 3, 3));

    assertThat(publisher.events()).singleElement()
        .isInstanceOfSatisfying(CampaignEventNotification.class, notification -> {
          assertThat(notification.eventType()).isEqualTo("CAMPAIGN_MATCH_POINTS");
          assertThat(notification.payload()).containsEntry("won", false)
              .containsEntry("pointsAwarded", 0).containsEntry("totalPoints", 100)
              .containsEntry("previousPosition", 3).containsEntry("newPosition", 3);
        });
  }

  private static final class RecordingPublisher implements ApplicationEventPublisher {

    private final List<ApplicationEvent> events = new ArrayList<>();

    @Override
    public void publish(final ApplicationEvent event) {

      this.events.add(event);
    }

    private List<ApplicationEvent> events() {

      return List.copyOf(this.events);
    }

  }

}
