package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.SpectatorMatchEventNotification;
import com.villo.truco.domain.model.match.events.AvailableActionsUpdatedEvent;
import com.villo.truco.domain.model.match.events.HandDealtEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.events.TurnChangedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.inmemory.InMemorySpectatorshipRepository;
import com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  private InMemoryBotVsBotMatchRegistry botVsBotMatchRegistry;
  private List<ApplicationEvent> publishedEvents;
  private SpectatorNotificationEventTranslator translator;

  @BeforeEach
  void setUp() {

    this.matchId = MatchId.generate();
    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
    this.spectator = PlayerId.generate();
    this.repository = new InMemorySpectatorshipRepository();
    this.botVsBotMatchRegistry = new InMemoryBotVsBotMatchRegistry();
    this.publishedEvents = new ArrayList<>();

    this.translator = new SpectatorNotificationEventTranslator(this.repository,
        this.botVsBotMatchRegistry, new MatchEventMapper(30_000L), this.publishedEvents::add);
  }

  private void withActiveSpectator() {

    final var spectatorship = Spectatorship.create(this.spectator);
    spectatorship.startWatching(this.matchId);
    this.repository.save(spectatorship);
  }

  private MatchEventEnvelope envelope(final com.villo.truco.domain.shared.DomainEventBase inner) {

    return new MatchEventEnvelope(this.matchId, this.playerOne, this.playerTwo, inner);
  }

  @Test
  @DisplayName("publica notificación cuando hay espectadores y evento no es SeatTargeted")
  void publishesForNonSeatTargetedEvent() {

    this.withActiveSpectator();

    this.translator.handle(this.envelope(new TurnChangedEvent(PlayerSeat.PLAYER_ONE)));

    assertThat(this.publishedEvents).hasSize(1);
    final var notification = (SpectatorMatchEventNotification) this.publishedEvents.getFirst();
    assertThat(notification.eventType()).isEqualTo("TURN_CHANGED");
  }

  @Test
  @DisplayName("no publica cuando no hay espectadores")
  void skipsWhenNoSpectators() {

    this.translator.handle(this.envelope(new TurnChangedEvent(PlayerSeat.PLAYER_ONE)));

    assertThat(this.publishedEvents).isEmpty();
  }

  @Test
  @DisplayName("en bot-vs-bot reenvía HAND_DEALT con ambas manos")
  void forwardsHandDealtForBotVsBot() {

    this.withActiveSpectator();
    this.botVsBotMatchRegistry.register(this.matchId, PlayerId.generate());

    this.translator.handle(this.envelope(
        new HandDealtEvent(Map.of(PlayerSeat.PLAYER_ONE, List.of(), PlayerSeat.PLAYER_TWO, List.of()))));

    assertThat(this.publishedEvents).hasSize(1);
    final var notification = (SpectatorMatchEventNotification) this.publishedEvents.getFirst();
    assertThat(notification.eventType()).isEqualTo("HAND_DEALT");
  }

  @Test
  @DisplayName("en bot-vs-bot reenvía PLAYER_HAND_UPDATED de ambos asientos")
  void forwardsPlayerHandUpdatedForBotVsBot() {

    this.withActiveSpectator();
    this.botVsBotMatchRegistry.register(this.matchId, PlayerId.generate());

    this.translator.handle(this.envelope(new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_ONE, List.of())));
    this.translator.handle(this.envelope(new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_TWO, List.of())));

    assertThat(this.publishedEvents).hasSize(2);
    assertThat(this.publishedEvents).allSatisfy(event -> assertThat(
        ((SpectatorMatchEventNotification) event).eventType()).isEqualTo("PLAYER_HAND_UPDATED"));
  }

  @Test
  @DisplayName("en partidas con humanos NO reenvía HAND_DEALT (cierre de fuga)")
  void doesNotForwardHandDealtForHumanMatch() {

    this.withActiveSpectator();

    this.translator.handle(this.envelope(
        new HandDealtEvent(Map.of(PlayerSeat.PLAYER_ONE, List.of(), PlayerSeat.PLAYER_TWO, List.of()))));

    assertThat(this.publishedEvents).isEmpty();
  }

  @Test
  @DisplayName("en partidas con humanos NO reenvía PLAYER_HAND_UPDATED")
  void doesNotForwardPlayerHandUpdatedForHumanMatch() {

    this.withActiveSpectator();

    this.translator.handle(this.envelope(new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_ONE, List.of())));

    assertThat(this.publishedEvents).isEmpty();
  }

  @Test
  @DisplayName("AVAILABLE_ACTIONS_UPDATED nunca llega al espectador, ni en bot-vs-bot")
  void neverForwardsAvailableActionsUpdated() {

    this.withActiveSpectator();
    this.botVsBotMatchRegistry.register(this.matchId, PlayerId.generate());

    this.translator.handle(
        this.envelope(new AvailableActionsUpdatedEvent(PlayerSeat.PLAYER_ONE, List.of())));

    assertThat(this.publishedEvents).isEmpty();
  }

}
