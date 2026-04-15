package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
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
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.support.TestPublicActorResolver;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateBotMatchCommandHandler")
class CreateBotMatchCommandHandlerTest {

  private static final BotPersonality DEFAULT_PERSONALITY = new BotPersonality(50, 50, 50, 50, 50);

  private static final MatchQueryRepository NO_ACTIVE_MATCH_REPO = new MatchQueryRepository() {
    @Override
    public Optional<Match> findById(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public boolean hasActiveMatch(final PlayerId playerId) {

      return false;
    }

    @Override
    public boolean hasUnfinishedMatch(final PlayerId playerId) {

      return false;
    }

    @Override
    public List<MatchId> findIdleMatchIds(final Instant idleSince) {

      return List.of();
    }

    @Override
    public List<Match> findPublicWaiting() {

      return List.of();
    }

    @Override
    public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

      return new CursorPageResult<>(findPublicWaiting(), null);
    }
  };

  private static final LeagueQueryRepository NO_LEAGUE_REPO = new LeagueQueryRepository() {
    @Override
    public Optional<League> findById(final LeagueId leagueId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findByMatchId(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findInProgressByPlayer(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public Optional<League> findWaitingByPlayer(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public List<LeagueId> findIdleLeagueIds(final Instant idleSince) {

      return List.of();
    }

    @Override
    public List<League> findPublicWaiting() {

      return List.of();
    }

    @Override
    public CursorPageResult<League> findPublicWaiting(final CursorPageQuery pageQuery) {

      return new CursorPageResult<>(findPublicWaiting(), null);
    }
  };

  private static final CupQueryRepository NO_CUP_REPO = new CupQueryRepository() {
    @Override
    public Optional<Cup> findById(final CupId cupId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findByMatchId(final MatchId matchId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findInProgressByPlayer(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public Optional<Cup> findWaitingByPlayer(final PlayerId playerId) {

      return Optional.empty();
    }

    @Override
    public List<CupId> findIdleCupIds(final Instant idleSince) {

      return List.of();
    }

    private List<Cup> findPublicWaiting() {

      return List.of();
    }

    @Override
    public CursorPageResult<Cup> findPublicWaiting(final CursorPageQuery pageQuery) {

      return new CursorPageResult<>(findPublicWaiting(), null);
    }
  };

  private static BotRegistry registryWith(final PlayerId botPlayerId) {

    final var profile = new BotProfile(botPlayerId, "bot", DEFAULT_PERSONALITY);
    return new BotRegistry() {
      @Override
      public boolean isBot(final PlayerId playerId) {

        return botPlayerId.equals(playerId);
      }

      @Override
      public Optional<BotProfile> getProfile(final PlayerId playerId) {

        return botPlayerId.equals(playerId) ? Optional.of(profile) : Optional.empty();
      }

      @Override
      public List<BotProfile> getAll() {

        return List.of(profile);
      }

      @Override
      public void register(final BotProfile botProfile) {

      }
    };
  }

  private static BotRegistry emptyRegistry() {

    return new BotRegistry() {
      @Override
      public boolean isBot(final PlayerId playerId) {

        return false;
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
  @DisplayName("crea y arranca el match contra bot sin generar chat asociado")
  void createsStartedBotMatchWithoutChat() {

    final var humanPlayer = PlayerId.generate();
    final var botPlayer = PlayerId.generate();
    final var botRegistry = registryWith(botPlayer);
    final var playerAvailabilityChecker = new PlayerAvailabilityChecker(NO_ACTIVE_MATCH_REPO,
        NO_LEAGUE_REPO, NO_CUP_REPO, botRegistry);
    final var chatRepository = new InMemoryChatRepository();
    final ChatEventNotifier chatEventNotifier = events -> {
    };
    final var chatHandler = new ChatMatchGameStartedEventHandler(botRegistry, chatRepository,
        chatRepository, chatEventNotifier);
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
    assertThat(chatRepository.findByParentTypeAndParentId(ChatParentType.MATCH,
        result.matchId())).isEmpty();

    final var queryHandler = new GetChatByParentQueryHandler(chatRepository,
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
    final var botRegistry = emptyRegistry();
    final var chatRepository = new InMemoryChatRepository();
    final ChatEventNotifier chatEventNotifier = events -> {
    };
    final var handler = new ChatMatchGameStartedEventHandler(botRegistry, chatRepository,
        chatRepository, chatEventNotifier);
    final var matchId = MatchId.generate();

    handler.handle(new GameStartedEvent(matchId, humanPlayerOne, humanPlayerTwo, 1));

    final ChatMessagesDTO dto = new GetChatByParentQueryHandler(chatRepository,
        TestPublicActorResolver.guestStyle()).handle(
        new GetChatByParentQuery(ChatParentType.MATCH, matchId.value().toString(), humanPlayerOne));

    assertThat(dto.parentType()).isEqualTo(ChatParentType.MATCH.name());
    assertThat(dto.parentId()).isEqualTo(matchId.value().toString());
  }

  private static final class InMemoryChatRepository implements ChatRepository, ChatQueryRepository {

    private final AtomicReference<Chat> storedChat = new AtomicReference<>();

    @Override
    public void save(final Chat chat) {

      this.storedChat.set(chat);
    }

    @Override
    public void delete(final ChatId chatId) {

      final var current = this.storedChat.get();
      if (current != null && current.getId().equals(chatId)) {
        this.storedChat.set(null);
      }
    }

    @Override
    public Optional<Chat> findById(final ChatId chatId) {

      final var current = this.storedChat.get();
      return current != null && current.getId().equals(chatId) ? Optional.of(current)
          : Optional.empty();
    }

    @Override
    public Optional<Chat> findByParentTypeAndParentId(final ChatParentType parentType,
        final String parentId) {

      final var current = this.storedChat.get();
      return current != null && current.getParentType() == parentType && current.getParentId()
          .equals(parentId) ? Optional.of(current) : Optional.empty();
    }

  }

}
