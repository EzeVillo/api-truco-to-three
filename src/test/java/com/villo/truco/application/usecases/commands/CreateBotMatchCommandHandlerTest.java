package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.CreateBotMatchCommand;
import com.villo.truco.application.dto.ChatMessagesDTO;
import com.villo.truco.application.eventhandlers.ChatMatchGameStartedEventHandler;
import com.villo.truco.application.exceptions.ChatNotFoundException;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.queries.GetChatByParentQuery;
import com.villo.truco.application.usecases.queries.GetChatByParentQueryHandler;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.support.TestPublicActorResolver;
import com.villo.truco.testutil.NoOpQuickMatchQueuePort;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateBotMatchCommandHandler")
class CreateBotMatchCommandHandlerTest {

  private static final BotPersonality DEFAULT_PERSONALITY = new BotPersonality(50, 50, 50, 50, 50);

  private static BotRegistry registryWith(final PlayerId botPlayerId) {

    final var profile = new BotProfile(botPlayerId, "bot", DEFAULT_PERSONALITY);
    final var registry = mock(BotRegistry.class);
    when(registry.isBot(botPlayerId)).thenReturn(true);
    when(registry.getProfile(botPlayerId)).thenReturn(Optional.of(profile));
    when(registry.getAll()).thenReturn(List.of(profile));
    return registry;
  }

  @Test
  @DisplayName("crea y arranca el match contra bot sin generar chat asociado")
  void createsStartedBotMatchWithoutChat() {

    final var humanPlayer = PlayerId.generate();
    final var botPlayer = PlayerId.generate();
    final var botRegistry = registryWith(botPlayer);
    final var matchRepo = mock(MatchQueryRepository.class);
    final var leagueRepo = mock(LeagueQueryRepository.class);
    final var cupRepo = mock(CupQueryRepository.class);
    final var rematchRepo = mock(RematchSessionRepository.class);
    when(rematchRepo.findOpenByPlayer(any())).thenReturn(Optional.empty());
    final var playerAvailabilityChecker = new PlayerAvailabilityChecker(matchRepo, leagueRepo,
        cupRepo, botRegistry, rematchRepo, NoOpQuickMatchQueuePort.INSTANCE);

    final var chatRepo = mock(ChatRepository.class);
    final var chatQueryRepo = mock(ChatQueryRepository.class);
    final ChatEventNotifier chatEventNotifier = events -> {
    };
    final var chatHandler = new ChatMatchGameStartedEventHandler(botRegistry, chatRepo,
        chatQueryRepo, chatEventNotifier);
    final MatchEventNotifier matchEventNotifier = events -> events.stream()
        .filter(GameStartedEvent.class::isInstance).map(GameStartedEvent.class::cast)
        .forEach(chatHandler::handle);
    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository matchRepository = savedMatch::set;
    final var handler = new CreateBotMatchCommandHandler(matchRepository, matchEventNotifier,
        botRegistry, playerAvailabilityChecker);

    final var result = handler.handle(
        new CreateBotMatchCommand(humanPlayer.value().toString(), 3, botPlayer.value().toString()));

    assertThat(result.matchId()).isNotBlank();
    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getStatus().name()).isEqualTo("IN_PROGRESS");
    assertThat(savedMatch.get().getPlayerOne()).isEqualTo(humanPlayer);
    assertThat(savedMatch.get().getPlayerTwo()).isEqualTo(botPlayer);
    assertThat(chatQueryRepo.findByParentTypeAndParentId(ChatParentType.MATCH,
        result.matchId())).isEmpty();

    final var queryHandler = new GetChatByParentQueryHandler(chatQueryRepo,
        TestPublicActorResolver.guestStyle());
    assertThatThrownBy(() -> queryHandler.handle(
        new GetChatByParentQuery(ChatParentType.MATCH, result.matchId(),
            humanPlayer))).isInstanceOf(ChatNotFoundException.class);
  }

  @Test
  @DisplayName("la consulta por parent sigue devolviendo chat para match humano")
  void getChatByParentStillReturnsHumanMatchChat() {

    final var humanPlayerOne = PlayerId.generate();
    final var humanPlayerTwo = PlayerId.generate();
    final var botRegistry = mock(BotRegistry.class);

    final var chatStore = new AtomicReference<Chat>();
    final var chatRepo = mock(ChatRepository.class);
    doAnswer(inv -> {
      chatStore.set(inv.getArgument(0));
      return null;
    }).when(chatRepo).save(any());
    final var chatQueryRepo = mock(ChatQueryRepository.class);
    when(chatQueryRepo.findByParentTypeAndParentId(any(), any())).thenAnswer(inv -> {
      final var chat = chatStore.get();
      if (chat == null) {
        return Optional.empty();
      }
      final var type = inv.getArgument(0, ChatParentType.class);
      final var parentId = inv.getArgument(1, String.class);
      return chat.getParentType() == type && chat.getParentId().equals(parentId) ? Optional.of(chat)
          : Optional.empty();
    });

    final ChatEventNotifier chatEventNotifier = events -> {
    };
    final var handler = new ChatMatchGameStartedEventHandler(botRegistry, chatRepo, chatQueryRepo,
        chatEventNotifier);
    final var matchId = MatchId.generate();

    handler.handle(new GameStartedEvent(matchId, humanPlayerOne, humanPlayerTwo, 1));

    final ChatMessagesDTO dto = new GetChatByParentQueryHandler(chatQueryRepo,
        TestPublicActorResolver.guestStyle()).handle(
        new GetChatByParentQuery(ChatParentType.MATCH, matchId.value().toString(), humanPlayerOne));

    assertThat(dto.parentType()).isEqualTo(ChatParentType.MATCH.name());
    assertThat(dto.parentId()).isEqualTo(matchId.value().toString());
  }

}
