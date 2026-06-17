package com.villo.truco.application.assemblers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.support.TestPublicActorResolver;
import com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpectatorMatchStateDTOAssembler")
class SpectatorMatchStateDTOAssemblerTest {

  private static Match startedMatch() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), false));
    match.startMatch(playerOne);
    match.startMatch(playerTwo);
    return match;
  }

  @Test
  @DisplayName("en bot-vs-bot expone las manos completas de ambos bots")
  void exposesBothHandsForBotVsBot() {

    final var match = startedMatch();
    final var registry = new InMemoryBotVsBotMatchRegistry();
    registry.register(match.getId(), PlayerId.generate());
    final var assembler = new SpectatorMatchStateDTOAssembler(TestPublicActorResolver.guestStyle(),
        registry, 30_000L);

    final var dto = assembler.toDto(match, 1);

    assertThat(dto.currentRound()).isNotNull();
    assertThat(dto.currentRound().handPlayerOne()).hasSize(3);
    assertThat(dto.currentRound().handPlayerTwo()).hasSize(3);
  }

  @Test
  @DisplayName("en partidas con humanos las manos quedan en null")
  void hidesHandsForHumanMatch() {

    final var match = startedMatch();
    final var assembler = new SpectatorMatchStateDTOAssembler(TestPublicActorResolver.guestStyle(),
        new InMemoryBotVsBotMatchRegistry(), 30_000L);

    final var dto = assembler.toDto(match, 1);

    assertThat(dto.currentRound()).isNotNull();
    assertThat(dto.currentRound().handPlayerOne()).isNull();
    assertThat(dto.currentRound().handPlayerTwo()).isNull();
  }

  @Test
  @DisplayName("expone quién cantó el envido en curso (el oponente del que debe responder)")
  void exposesEnvidoCaller() {

    final var match = startedMatch();
    final var caller = match.getCurrentTurn();
    match.callEnvido(caller, EnvidoCall.ENVIDO);
    final var assembler = new SpectatorMatchStateDTOAssembler(TestPublicActorResolver.guestStyle(),
        new InMemoryBotVsBotMatchRegistry(), 30_000L);

    final var dto = assembler.toDto(match, 1);

    assertThat(dto.currentRound().currentEnvidoCall()).isEqualTo("ENVIDO");
    assertThat(dto.currentRound().currentEnvidoCaller()).isEqualTo(
        TestPublicActorResolver.displayName(caller));
    assertThat(dto.currentRound().currentEnvidoCaller()).isNotEqualTo(
        dto.currentRound().currentTurn());
    assertThat(dto.currentRound().currentTrucoCaller()).isNull();
  }

  @Test
  @DisplayName("sin canto en curso, ambos cantores quedan en null")
  void noCallerWhenNoCallInProgress() {

    final var match = startedMatch();
    final var assembler = new SpectatorMatchStateDTOAssembler(TestPublicActorResolver.guestStyle(),
        new InMemoryBotVsBotMatchRegistry(), 30_000L);

    final var dto = assembler.toDto(match, 1);

    assertThat(dto.currentRound().currentEnvidoCaller()).isNull();
    assertThat(dto.currentRound().currentTrucoCaller()).isNull();
  }

}
