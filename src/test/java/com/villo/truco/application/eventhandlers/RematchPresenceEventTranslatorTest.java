package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.villo.truco.domain.model.rematch.events.RematchSessionClosedByLeaveEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionConfirmedEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionOpenedEvent;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.services.FriendAvailabilityChangeNotifier;
import java.time.Instant;
import java.util.Collection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("RematchPresenceEventTranslator")
class RematchPresenceEventTranslatorTest {

  @Test
  @DisplayName("ante REMATCH_AVAILABLE notifica a ambos jugadores de la sesion")
  void notifiesOnRematchOpened() {

    final var notifier = mock(PresenceNotifier.class);
    final var friendNotifier = mock(FriendAvailabilityChangeNotifier.class);
    final var translator = new RematchPresenceEventTranslator(notifier, friendNotifier);
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();

    translator.handle(
        new RematchSessionOpenedEvent(RematchSessionId.generate(), MatchId.generate(), p1, p2,
            Instant.now(), false, false));

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(p1, p2);
    verifyNoInteractions(friendNotifier);
  }

  @Test
  @DisplayName("ante REMATCH_CONFIRMED notifica a los nuevos jugadores (caso liberacion)")
  void notifiesOnRematchConfirmed() {

    final var notifier = mock(PresenceNotifier.class);
    final var translator = new RematchPresenceEventTranslator(notifier,
        mock(FriendAvailabilityChangeNotifier.class));
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();

    translator.handle(
        new RematchSessionConfirmedEvent(RematchSessionId.generate(), MatchId.generate(),
            MatchId.generate(), p1, p2, 3));

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(p1, p2);
  }

  @Test
  @DisplayName("ante rechazo de revancha publica disponibilidad actualizada a amigos de ambos jugadores")
  void publishesAvailabilityWhenRematchIsClosedByLeave() {

    final var notifier = mock(PresenceNotifier.class);
    final var friendNotifier = mock(FriendAvailabilityChangeNotifier.class);
    final var translator = new RematchPresenceEventTranslator(notifier, friendNotifier);
    final var actor = PlayerId.generate();
    final var otherPlayer = PlayerId.generate();
    final var event = new RematchSessionClosedByLeaveEvent(RematchSessionId.generate(),
        MatchId.generate(), actor, otherPlayer);

    translator.handle(event);

    final ArgumentCaptor<Collection<PlayerId>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(notifier).notifyPlayers(captor.capture());
    assertThat(captor.getValue()).containsExactlyInAnyOrder(actor, otherPlayer);
    verify(friendNotifier).notifyAvailabilityChanged(actor, event.getTimestamp());
    verify(friendNotifier).notifyAvailabilityChanged(otherPlayer, event.getTimestamp());
  }

  @Test
  @DisplayName("ante un evento de revancha no contemplado no notifica")
  void ignoresOtherRematchEvents() {

    final var notifier = mock(PresenceNotifier.class);
    final var friendNotifier = mock(FriendAvailabilityChangeNotifier.class);
    final var translator = new RematchPresenceEventTranslator(notifier, friendNotifier);

    translator.handle(new OtherRematchEvent(RematchSessionId.generate(), MatchId.generate()));

    verifyNoInteractions(notifier);
    verifyNoInteractions(friendNotifier);
  }

  private static final class OtherRematchEvent extends RematchSessionDomainEvent {

    OtherRematchEvent(final RematchSessionId rematchSessionId, final MatchId originMatchId) {

      super("OTHER", rematchSessionId, originMatchId);
    }

  }

}
