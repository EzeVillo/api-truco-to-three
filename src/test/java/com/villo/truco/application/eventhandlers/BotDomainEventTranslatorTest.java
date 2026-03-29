package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.BotTurnRequired;
import com.villo.truco.application.events.PostCommitApplicationEvent;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.RoundStartedEvent;
import com.villo.truco.domain.model.match.events.TurnChangedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BotDomainEventTranslator")
class BotDomainEventTranslatorTest {

  private final List<ApplicationEvent> published = new ArrayList<>();
  private final ApplicationEventPublisher publisher = published::add;

  private static BotRegistry registryWith(final PlayerId botPlayerId) {

    return new BotRegistry() {
      @Override
      public boolean isBot(final PlayerId playerId) {

        return botPlayerId.equals(playerId);
      }

      @Override
      public Optional<BotProfile> getProfile(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<BotProfile> getAll() {

        return List.of();
      }

      @Override
      public void register(final BotProfile profile) {

      }
    };
  }

  @Test
  @DisplayName("TurnChanged para jugador bot publica BotTurnRequired")
  void turnChangedForBotPublishesBotTurnRequired() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var botPlayer = PlayerId.generate();
    final var botRegistry = mock(BotRegistry.class);
    when(botRegistry.isBot(botPlayer)).thenReturn(true);
    final var translator = new BotDomainEventTranslator(botRegistry, publisher);
    final var inner = new TurnChangedEvent(PlayerSeat.PLAYER_TWO);
    final var envelope = new MatchEventEnvelope(matchId, p1, botPlayer, inner);

    translator.handle(envelope);

    assertThat(published).hasSize(1);
    final var event = (BotTurnRequired) published.getFirst();
    assertThat(event).isInstanceOf(PostCommitApplicationEvent.class);
    assertThat(event.matchId()).isEqualTo(matchId);
    assertThat(event.botPlayerId()).isEqualTo(botPlayer);
  }

  @Test
  @DisplayName("TurnChanged para jugador humano no publica nada")
  void turnChangedForHumanPublishesNothing() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var botRegistry = mock(BotRegistry.class);
    when(botRegistry.isBot(p1)).thenReturn(false);
    final var translator = new BotDomainEventTranslator(botRegistry, publisher);
    final var inner = new TurnChangedEvent(PlayerSeat.PLAYER_ONE);
    final var envelope = new MatchEventEnvelope(matchId, p1, p2, inner);

    translator.handle(envelope);

    assertThat(published).isEmpty();
  }

  @Test
  @DisplayName("RoundStarted para bot mano publica BotTurnRequired")
  void roundStartedForBotPublishesBotTurnRequired() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var botPlayer = PlayerId.generate();
    final var botRegistry = mock(BotRegistry.class);
    when(botRegistry.isBot(botPlayer)).thenReturn(true);
    final var translator = new BotDomainEventTranslator(botRegistry, publisher);
    final var inner = new RoundStartedEvent(1, PlayerSeat.PLAYER_TWO);
    final var envelope = new MatchEventEnvelope(matchId, p1, botPlayer, inner);

    translator.handle(envelope);

    assertThat(published).hasSize(1);
    final var event = (BotTurnRequired) published.getFirst();
    assertThat(event.matchId()).isEqualTo(matchId);
    assertThat(event.botPlayerId()).isEqualTo(botPlayer);
  }

  @Test
  @DisplayName("RoundStarted para humano mano no publica nada")
  void roundStartedForHumanPublishesNothing() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var botRegistry = mock(BotRegistry.class);
    when(botRegistry.isBot(p1)).thenReturn(false);
    final var translator = new BotDomainEventTranslator(botRegistry, publisher);
    final var inner = new RoundStartedEvent(1, PlayerSeat.PLAYER_ONE);
    final var envelope = new MatchEventEnvelope(matchId, p1, p2, inner);

    translator.handle(envelope);

    assertThat(published).isEmpty();
  }

  @Test
  @DisplayName("al empezar el segundo game con bot mano publica BotTurnRequired")
  void secondGameStartWithBotAsManoPublishesBotTurnRequired() {

    final var humanPlayer = PlayerId.generate();
    final var botPlayer = PlayerId.generate();
    final var translator = new BotDomainEventTranslator(registryWith(botPlayer), publisher);
    final var match = Match.createReady(humanPlayer, botPlayer,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));

    match.startMatch(humanPlayer);
    match.startMatch(botPlayer);

    match.callTruco(humanPlayer);
    match.rejectTruco(botPlayer);

    final var botCard = match.getCardsOf(botPlayer).getFirst();
    match.playCard(botPlayer, botCard);
    match.callTruco(humanPlayer);
    match.rejectTruco(botPlayer);

    match.callTruco(humanPlayer);
    match.clearDomainEvents();
    published.clear();

    match.rejectTruco(botPlayer);
    match.getMatchDomainEvents().forEach(translator::handle);

    assertThat(match.getCurrentTurn()).isEqualTo(botPlayer);
    assertThat(published).singleElement().isInstanceOfSatisfying(BotTurnRequired.class, event -> {
      assertThat(event.matchId()).isEqualTo(match.getId());
      assertThat(event.botPlayerId()).isEqualTo(botPlayer);
    });
  }

  @Test
  @DisplayName("evento no relacionado no publica nada")
  void nonTurnChangedOrRoundStartedEventPublishesNothing() {

    final var matchId = MatchId.generate();
    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var botRegistry = mock(BotRegistry.class);
    final var translator = new BotDomainEventTranslator(botRegistry, publisher);

    translator.handle(new PlayerJoinedEvent(matchId, p1, p2));

    assertThat(published).isEmpty();
  }

}
