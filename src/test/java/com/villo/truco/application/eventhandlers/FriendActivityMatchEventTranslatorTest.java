package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendActivityDTO;
import com.villo.truco.social.application.dto.SpectatableMatchRefDTO;
import com.villo.truco.social.application.events.FriendActivityNotification;
import com.villo.truco.social.application.services.FriendActivityResolver;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FriendActivityMatchEventTranslator")
class FriendActivityMatchEventTranslatorTest {

  @Test
  @DisplayName("ante primer GAME_STARTED publica actividad espectable para amigos")
  void publishesSpectatableActivityOnFirstGameStarted() {

    final var resolver = mock(FriendActivityResolver.class);
    final var publisher = new FriendActivityEventHandler();
    final var translator = new FriendActivityMatchEventTranslator(resolver, publisher);
    final var matchId = MatchId.generate();
    final var player = PlayerId.generate();
    final var rival = PlayerId.generate();
    final var friend = PlayerId.generate();
    final var ref = new SpectatableMatchRefDTO(matchId.value().toString(), "IN_PROGRESS");
    when(resolver.resolveMatchActivityChangesByRecipient(matchId, player, rival, player,
        true)).thenReturn(Map.of(friend, new FriendActivityDTO("martina", ref)));

    translator.handle(new GameStartedEvent(matchId, player, rival, 1));

    assertThat(publisher.events()).hasSize(1);
    assertThat(publisher.events()).filteredOn(FriendActivityNotification.class::isInstance)
        .singleElement().isInstanceOfSatisfying(FriendActivityNotification.class, event -> {
          assertThat(event.recipients()).containsExactly(friend);
          assertThat(event.eventType()).isEqualTo("FRIEND_ACTIVITY_CHANGED");
          assertThat(event.payload()).containsEntry("friendUsername", "martina");
          assertThat(event.payload()).containsEntry("spectatableMatch", ref);
        });
  }

  @Test
  @DisplayName("ante MATCH_FINISHED publica baja de actividad para amigos")
  void publishesNullActivityOnMatchFinished() {

    final var resolver = mock(FriendActivityResolver.class);
    final var publisher = new FriendActivityEventHandler();
    final var translator = new FriendActivityMatchEventTranslator(resolver, publisher);
    final var matchId = MatchId.generate();
    final var player = PlayerId.generate();
    final var rival = PlayerId.generate();
    final var friend = PlayerId.generate();
    when(resolver.resolveMatchActivityChangesByRecipient(matchId, player, rival, player,
        false)).thenReturn(Map.of(friend, new FriendActivityDTO("martina", null)));

    translator.handle(new MatchFinishedEvent(matchId, player, rival, PlayerSeat.PLAYER_ONE, 2, 0));

    assertThat(publisher.events()).hasSize(1);
    assertThat(publisher.events()).filteredOn(FriendActivityNotification.class::isInstance)
        .singleElement().isInstanceOfSatisfying(FriendActivityNotification.class, event -> {
          assertThat(event.recipients()).containsExactly(friend);
          assertThat(event.payload()).containsEntry("friendUsername", "martina");
          assertThat(event.payload()).containsEntry("spectatableMatch", null);
        });
  }

}
