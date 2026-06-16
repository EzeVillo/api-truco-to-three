package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.CreateBotVsBotMatchCommand;
import com.villo.truco.application.exceptions.BotNotFoundException;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.model.match.exceptions.SamePlayerMatchException;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.MatchTimeoutEntry;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry;
import com.villo.truco.testutil.NoOpQuickMatchQueuePort;
import com.villo.truco.testutil.NoOpSpectatorshipRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateBotVsBotMatchCommandHandler")
class CreateBotVsBotMatchCommandHandlerTest {

  private static final BotPersonality DEFAULT_PERSONALITY = new BotPersonality(50, 50, 50, 50, 50);

  private static BotRegistry registryWith(final PlayerId... bots) {

    final var registry = mock(BotRegistry.class);
    for (final var bot : bots) {
      final var profile = new BotProfile(bot, "bot", DEFAULT_PERSONALITY);
      when(registry.isBot(bot)).thenReturn(true);
      when(registry.getProfile(bot)).thenReturn(Optional.of(profile));
    }
    return registry;
  }

  private static PlayerAvailabilityChecker checker(final boolean ownerBusy,
      final BotRegistry botRegistry) {

    final var matchRepo = mock(MatchQueryRepository.class);
    when(matchRepo.hasUnfinishedMatch(any())).thenReturn(ownerBusy);
    final var leagueRepo = mock(com.villo.truco.domain.ports.LeagueQueryRepository.class);
    when(leagueRepo.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(leagueRepo.findWaitingByPlayer(any())).thenReturn(Optional.empty());
    final var cupRepo = mock(com.villo.truco.domain.ports.CupQueryRepository.class);
    when(cupRepo.findInProgressByPlayer(any())).thenReturn(Optional.empty());
    when(cupRepo.findWaitingByPlayer(any())).thenReturn(Optional.empty());
    final var rematchRepo = mock(com.villo.truco.domain.ports.RematchSessionRepository.class);
    when(rematchRepo.findOpenByPlayer(any())).thenReturn(Optional.empty());
    return new PlayerAvailabilityChecker(matchRepo, leagueRepo, cupRepo, botRegistry, rematchRepo,
        NoOpQuickMatchQueuePort.INSTANCE, NoOpSpectatorshipRepository.INSTANCE, new com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry());
  }

  private static MatchRepository capturingRepository(final AtomicReference<Match> sink) {

    return new MatchRepository() {
      @Override
      public void save(final Match match) {

        sink.set(match);
      }

      @Override
      public Stream<MatchTimeoutEntry> findActiveWithTimeoutDeadline() {

        return Stream.empty();
      }
    };
  }

  @Test
  @DisplayName("crea e inicia el match entre dos bots y registra al dueño")
  void createsStartedMatchAndRegistersOwner() {

    final var owner = PlayerId.generate();
    final var botOne = PlayerId.generate();
    final var botTwo = PlayerId.generate();
    final var botRegistry = registryWith(botOne, botTwo);
    final var registry = new InMemoryBotVsBotMatchRegistry();
    final var savedMatch = new AtomicReference<Match>();
    final MatchEventNotifier notifier = events -> {
    };
    final var handler = new CreateBotVsBotMatchCommandHandler(capturingRepository(savedMatch),
        notifier, botRegistry, registry, checker(false, botRegistry));

    final var result = handler.handle(
        new CreateBotVsBotMatchCommand(owner.value().toString(), 3, botOne.value().toString(),
            botTwo.value().toString()));

    assertThat(result.matchId()).isNotBlank();
    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getStatus().name()).isEqualTo("IN_PROGRESS");
    assertThat(savedMatch.get().getPlayerOne()).isEqualTo(botOne);
    assertThat(savedMatch.get().getPlayerTwo()).isEqualTo(botTwo);
    assertThat(registry.isBotVsBotMatch(savedMatch.get().getId())).isTrue();
    assertThat(registry.findOwnerByMatchId(savedMatch.get().getId())).contains(owner);
  }

  @Test
  @DisplayName("rechaza si ambos bots son iguales")
  void rejectsSameBotTwice() {

    final var owner = PlayerId.generate();
    final var bot = PlayerId.generate();
    final var botRegistry = registryWith(bot);
    final var handler = new CreateBotVsBotMatchCommandHandler(
        capturingRepository(new AtomicReference<>()), events -> {
    }, botRegistry, new InMemoryBotVsBotMatchRegistry(), checker(false, botRegistry));

    assertThatThrownBy(() -> handler.handle(
        new CreateBotVsBotMatchCommand(owner.value().toString(), 3, bot.value().toString(),
            bot.value().toString()))).isInstanceOf(SamePlayerMatchException.class);
  }

  @Test
  @DisplayName("rechaza si alguno de los bots no existe en el catálogo")
  void rejectsUnknownBot() {

    final var owner = PlayerId.generate();
    final var botOne = PlayerId.generate();
    final var unknownBot = PlayerId.generate();
    final var botRegistry = registryWith(botOne);
    final var handler = new CreateBotVsBotMatchCommandHandler(
        capturingRepository(new AtomicReference<>()), events -> {
    }, botRegistry, new InMemoryBotVsBotMatchRegistry(), checker(false, botRegistry));

    assertThatThrownBy(() -> handler.handle(
        new CreateBotVsBotMatchCommand(owner.value().toString(), 3, botOne.value().toString(),
            unknownBot.value().toString()))).isInstanceOf(BotNotFoundException.class);
  }

  @Test
  @DisplayName("rechaza si el creador está ocupado")
  void rejectsWhenOwnerBusy() {

    final var owner = PlayerId.generate();
    final var botOne = PlayerId.generate();
    final var botTwo = PlayerId.generate();
    final var botRegistry = registryWith(botOne, botTwo);
    final var handler = new CreateBotVsBotMatchCommandHandler(
        capturingRepository(new AtomicReference<>()), events -> {
    }, botRegistry, new InMemoryBotVsBotMatchRegistry(), checker(true, botRegistry));

    assertThatThrownBy(() -> handler.handle(
        new CreateBotVsBotMatchCommand(owner.value().toString(), 3, botOne.value().toString(),
            botTwo.value().toString()))).isInstanceOf(PlayerAlreadyInActiveMatchException.class);
  }

}
