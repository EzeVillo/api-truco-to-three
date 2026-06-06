package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.villo.truco.domain.model.league.events.LeagueCreatedEvent;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.LeagueMatchActivatedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerLeftEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.services.FriendPresenceAvailabilityNotifier;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("LeaguePresenceEventTranslator")
class LeaguePresenceEventTranslatorTest {

  @Test
  @DisplayName("ante LEAGUE_CREATED notifica al creador (host a la espera, ya ocupado)")
  void notifiesOnLeagueCreated() {

    final var notifier = mock(PresenceNotifier.class);
    final var friendNotifier = mock(FriendPresenceAvailabilityNotifier.class);
    final var translator = new LeaguePresenceEventTranslator(notifier, friendNotifier);
    final var creator = PlayerId.generate();
    final var event = new LeagueCreatedEvent(LeagueId.generate(), List.of(creator));

    translator.handle(event);

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactly(creator);
    verify(friendNotifier).notifyAvailabilityChanged(creator, event.getTimestamp());
  }

  @Test
  @DisplayName("ante LEAGUE_MATCH_ACTIVATED notifica a todos los participantes")
  void notifiesParticipantsOnMatchActivated() {

    final var notifier = mock(PresenceNotifier.class);
    final var translator = new LeaguePresenceEventTranslator(notifier,
        mock(FriendPresenceAvailabilityNotifier.class));
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();

    translator.handle(
        new LeagueMatchActivatedEvent(LeagueId.generate(), List.of(p1, p2), MatchId.generate()));

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(p1, p2);
  }

  @Test
  @DisplayName("ante LEAGUE_PLAYER_LEFT notifica a los participantes mas el jugador que se fue")
  void notifiesLeaverOnPlayerLeft() {

    final var notifier = mock(PresenceNotifier.class);
    final var translator = new LeaguePresenceEventTranslator(notifier,
        mock(FriendPresenceAvailabilityNotifier.class));
    final var remaining = PlayerId.generate();
    final var leaver = PlayerId.generate();

    translator.handle(new LeaguePlayerLeftEvent(LeagueId.generate(), List.of(remaining), leaver));

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(remaining, leaver);
  }

  @Test
  @DisplayName("ante un evento que no es transicion de ocupacion no notifica")
  void ignoresNonOccupancyEvents() {

    final var notifier = mock(PresenceNotifier.class);
    final var friendNotifier = mock(FriendPresenceAvailabilityNotifier.class);
    final var translator = new LeaguePresenceEventTranslator(notifier, friendNotifier);

    translator.handle(new OtherLeagueEvent(LeagueId.generate(), List.of(PlayerId.generate())));

    verifyNoInteractions(notifier);
    verifyNoInteractions(friendNotifier);
  }

  private static final class OtherLeagueEvent extends LeagueDomainEvent {

    OtherLeagueEvent(final LeagueId leagueId, final List<PlayerId> participants) {

      super("OTHER", leagueId, participants);
    }

  }

}
