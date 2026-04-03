package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.SpectatorMatchEventNotification;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.events.TurnChangedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.inmemory.InMemorySpectatorshipRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpectatorNotificationEventTranslator")
class SpectatorNotificationEventTranslatorTest {

  private MatchId matchId;
  private PlayerId playerOne;
  private PlayerId playerTwo;
  private PlayerId spectator;
  private InMemorySpectatorshipRepository repository;
  private List<ApplicationEvent> publishedEvents;
  private SpectatorNotificationEventTranslator translator;

  @BeforeEach
  void setUp() {

    this.matchId = MatchId.generate();
    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
    this.spectator = PlayerId.generate();
    this.repository = new InMemorySpectatorshipRepository();
    this.publishedEvents = new ArrayList<>();

    this.translator = new SpectatorNotificationEventTranslator(this.repository,
        new MatchEventMapper(), this.publishedEvents::add);
  }

  @Test
  @DisplayName("publica notificación cuando hay espectadores y evento no es SeatTargeted")
  void publishesForNonSeatTargetedEvent() {

    final var spectatorship = Spectatorship.create(this.spectator);
    spectatorship.startWatching(this.matchId);
    this.repository.save(spectatorship);

    final var inner = new TurnChangedEvent(PlayerSeat.PLAYER_ONE);
    final var envelope = new MatchEventEnvelope(this.matchId, this.playerOne, this.playerTwo,
        inner);

    this.translator.handle(envelope);

    assertThat(this.publishedEvents).hasSize(1);
    assertThat(this.publishedEvents.getFirst()).isInstanceOf(SpectatorMatchEventNotification.class);

    final var notification = (SpectatorMatchEventNotification) this.publishedEvents.getFirst();
    assertThat(notification.matchId()).isEqualTo(this.matchId);
    assertThat(notification.spectatorIds()).containsExactly(this.spectator);
    assertThat(notification.eventType()).isEqualTo("TURN_CHANGED");
  }

  @Test
  @DisplayName("no publica cuando el evento es SeatTargeted")
  void skipsSeatTargetedEvent() {

    final var spectatorship = Spectatorship.create(this.spectator);
    spectatorship.startWatching(this.matchId);
    this.repository.save(spectatorship);

    final var inner = new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_ONE, List.of());
    final var envelope = new MatchEventEnvelope(this.matchId, this.playerOne, this.playerTwo,
        inner);

    this.translator.handle(envelope);

    assertThat(this.publishedEvents).isEmpty();
  }

  @Test
  @DisplayName("no publica cuando no hay espectadores")
  void skipsWhenNoSpectators() {

    final var inner = new TurnChangedEvent(PlayerSeat.PLAYER_ONE);
    final var envelope = new MatchEventEnvelope(this.matchId, this.playerOne, this.playerTwo,
        inner);

    this.translator.handle(envelope);

    assertThat(this.publishedEvents).isEmpty();
  }

}
