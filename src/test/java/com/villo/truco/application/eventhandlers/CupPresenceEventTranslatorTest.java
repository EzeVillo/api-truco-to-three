package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.villo.truco.domain.model.cup.events.CupCreatedEvent;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.model.cup.events.CupMatchActivatedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerLeftEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.services.FriendPresenceAvailabilityNotifier;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("CupPresenceEventTranslator")
class CupPresenceEventTranslatorTest {

  @Test
  @DisplayName("ante CUP_CREATED notifica al creador (host a la espera, ya ocupado)")
  void notifiesOnCupCreated() {

    final var notifier = mock(PresenceNotifier.class);
    final var friendNotifier = mock(FriendPresenceAvailabilityNotifier.class);
    final var translator = new CupPresenceEventTranslator(notifier, friendNotifier);
    final var creator = PlayerId.generate();
    final var event = new CupCreatedEvent(CupId.generate(), List.of(creator));

    translator.handle(event);

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactly(creator);
    verify(friendNotifier).notifyAvailabilityChanged(creator, event.getTimestamp());
  }

  @Test
  @DisplayName("ante CUP_MATCH_ACTIVATED notifica a todos los participantes")
  void notifiesParticipantsOnMatchActivated() {

    final var notifier = mock(PresenceNotifier.class);
    final var translator = new CupPresenceEventTranslator(notifier,
        mock(FriendPresenceAvailabilityNotifier.class));
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();

    translator.handle(
        new CupMatchActivatedEvent(CupId.generate(), List.of(p1, p2), MatchId.generate()));

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(p1, p2);
  }

  @Test
  @DisplayName("ante CUP_PLAYER_LEFT notifica a los participantes mas el jugador que se fue")
  void notifiesLeaverOnPlayerLeft() {

    final var notifier = mock(PresenceNotifier.class);
    final var translator = new CupPresenceEventTranslator(notifier,
        mock(FriendPresenceAvailabilityNotifier.class));
    final var remaining = PlayerId.generate();
    final var leaver = PlayerId.generate();

    translator.handle(new CupPlayerLeftEvent(CupId.generate(), List.of(remaining), leaver));

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(remaining, leaver);
  }

  @Test
  @DisplayName("ante un evento que no es transicion de ocupacion no notifica")
  void ignoresNonOccupancyEvents() {

    final var notifier = mock(PresenceNotifier.class);
    final var friendNotifier = mock(FriendPresenceAvailabilityNotifier.class);
    final var translator = new CupPresenceEventTranslator(notifier, friendNotifier);

    translator.handle(new OtherCupEvent(CupId.generate(), List.of(PlayerId.generate())));

    verifyNoInteractions(notifier);
    verifyNoInteractions(friendNotifier);
  }

  private static final class OtherCupEvent extends CupDomainEvent {

    OtherCupEvent(final CupId cupId, final List<PlayerId> participants) {

      super("OTHER", cupId, participants);
    }

  }

}
