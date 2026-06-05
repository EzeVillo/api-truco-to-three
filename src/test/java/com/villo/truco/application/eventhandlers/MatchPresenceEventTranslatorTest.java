package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Collection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("MatchPresenceEventTranslator")
class MatchPresenceEventTranslatorTest {

  @Test
  @DisplayName("ante PLAYER_JOINED notifica a ambos jugadores de la partida")
  void notifiesOnPlayerJoined() {

    final var notifier = mock(PresenceNotifier.class);
    final var translator = new MatchPresenceEventTranslator(notifier);
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    translator.handle(new PlayerJoinedEvent(MatchId.generate(), playerOne, playerTwo));

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(playerOne, playerTwo);
  }

  @Test
  @DisplayName("ante MATCH_FINISHED notifica a los jugadores (caso liberacion)")
  void notifiesOnMatchFinished() {

    final var notifier = mock(PresenceNotifier.class);
    final var translator = new MatchPresenceEventTranslator(notifier);
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    translator.handle(
        new MatchFinishedEvent(MatchId.generate(), playerOne, playerTwo, PlayerSeat.PLAYER_ONE, 2,
            0));

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(playerOne, playerTwo);
  }

  @Test
  @DisplayName("ante un evento que no es transicion de ocupacion no notifica")
  void ignoresNonOccupancyEvents() {

    final var notifier = mock(PresenceNotifier.class);
    final var translator = new MatchPresenceEventTranslator(notifier);

    translator.handle(
        new OtherMatchEvent(MatchId.generate(), PlayerId.generate(), PlayerId.generate()));

    verifyNoInteractions(notifier);
  }

  private static final class OtherMatchEvent extends MatchDomainEvent {

    OtherMatchEvent(final MatchId matchId, final PlayerId playerOne, final PlayerId playerTwo) {

      super("OTHER", matchId, playerOne, playerTwo);
    }

  }

}
