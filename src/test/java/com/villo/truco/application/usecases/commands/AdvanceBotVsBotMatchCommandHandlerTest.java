package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.AdvanceBotVsBotMatchCommand;
import com.villo.truco.application.commands.ExecuteBotTurnCommand;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.application.ports.in.ExecuteBotTurnUseCase;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.AdvanceBotMatchNotOwnerException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchLockingRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdvanceBotVsBotMatchCommandHandler")
class AdvanceBotVsBotMatchCommandHandlerTest {

  private PlayerId owner;
  private PlayerId botOne;
  private PlayerId botTwo;
  private Match match;
  private InMemoryBotVsBotMatchRegistry registry;
  private List<ExecuteBotTurnCommand> executed;

  @BeforeEach
  void setUp() {

    this.owner = PlayerId.generate();
    this.botOne = PlayerId.generate();
    this.botTwo = PlayerId.generate();
    this.match = Match.createReady(this.botOne, this.botTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), false));
    this.match.startMatch(this.botOne);
    this.match.startMatch(this.botTwo);
    this.match.clearDomainEvents();
    this.registry = new InMemoryBotVsBotMatchRegistry();
    this.registry.register(this.match.getId(), this.owner);
    this.executed = new ArrayList<>();
  }

  private AdvanceBotVsBotMatchCommandHandler handler() {

    return this.handlerWith(this.registry, matchId -> Optional.of(this.match));
  }

  private AdvanceBotVsBotMatchCommandHandler handlerWith(
      final InMemoryBotVsBotMatchRegistry botVsBotRegistry,
      final MatchLockingRepository lockingRepo) {

    final ExecuteBotTurnUseCase executeBotTurn = command -> {
      this.executed.add(command);
      return null;
    };
    return new AdvanceBotVsBotMatchCommandHandler(botVsBotRegistry, lockingRepo, executeBotTurn);
  }

  private AdvanceBotVsBotMatchCommand commandFrom(final PlayerId requester) {

    return new AdvanceBotVsBotMatchCommand(this.match.getId().value().toString(),
        requester.value().toString());
  }

  @Test
  @DisplayName("el dueño avanza → ejecuta una única acción del bot al que le toca")
  void ownerAdvancesExecutesOneBotTurn() {

    this.handler().handle(commandFrom(this.owner));

    assertThat(this.executed).hasSize(1);
    assertThat(this.executed.getFirst().matchId()).isEqualTo(this.match.getId());
    assertThat(this.executed.getFirst().botPlayerId()).isEqualTo(this.match.getCurrentTurn());
  }

  @Test
  @DisplayName("un usuario que no es el creador es rechazado y no ejecuta nada")
  void nonOwnerRejected() {

    final var stranger = PlayerId.generate();

    assertThatThrownBy(() -> this.handler().handle(commandFrom(stranger))).isInstanceOf(
        AdvanceBotMatchNotOwnerException.class);

    assertThat(this.executed).isEmpty();
  }

  @Test
  @DisplayName("si el match no está registrado como bot-vs-bot, se rechaza por no ser el dueño")
  void unknownBotVsBotMatchRejected() {

    final var handler = this.handlerWith(new InMemoryBotVsBotMatchRegistry(),
        matchId -> Optional.of(this.match));

    assertThatThrownBy(() -> handler.handle(commandFrom(this.owner))).isInstanceOf(
        AdvanceBotMatchNotOwnerException.class);

    assertThat(this.executed).isEmpty();
  }

  @Test
  @DisplayName("si el dueño es correcto pero el match no existe, lanza MatchNotFoundException")
  void ownerOkButMatchMissing() {

    final var handler = this.handlerWith(this.registry, matchId -> Optional.empty());

    assertThatThrownBy(() -> handler.handle(commandFrom(this.owner))).isInstanceOf(
        MatchNotFoundException.class);
  }

  @Test
  @DisplayName("idempotente: si la serie ya terminó, no ejecuta ninguna acción")
  void idempotentWhenAlreadyFinished() {

    this.match.abandon(this.botOne);
    this.match.clearDomainEvents();

    this.handler().handle(commandFrom(this.owner));

    assertThat(this.executed).isEmpty();
  }

}
