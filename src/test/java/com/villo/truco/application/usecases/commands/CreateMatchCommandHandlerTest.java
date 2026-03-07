package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.InvalidMatchRulesException;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateMatchCommandHandler")
class CreateMatchCommandHandlerTest {

  @Test
  @DisplayName("crea partida con gamesToPlay valido")
  void createsMatch() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final PlayerTokenProvider tokenProvider = (matchId, playerId) -> "token";
    final var handler = new CreateMatchCommandHandler(repository, tokenProvider);

    handler.handle(new CreateMatchCommand(3));

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getId()).isNotNull();
    assertThat(savedMatch.get().getInviteCode()).isNotNull();
  }

  @Test
  @DisplayName("falla si gamesToPlay no es 1, 3 o 5")
  void failsWhenGamesToPlayIsInvalid() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final PlayerTokenProvider tokenProvider = (matchId, playerId) -> "token";
    final var handler = new CreateMatchCommandHandler(repository, tokenProvider);

    assertThatThrownBy(() -> handler.handle(new CreateMatchCommand(7))).isInstanceOf(
        InvalidMatchRulesException.class).hasMessage("gamesToPlay must be one of: 1, 3, 5");

    assertThat(savedMatch.get()).isNull();
  }

  @Test
  @DisplayName("genera token con id de match creado y player host")
  void generatesTokenForCreatedMatchAndHostPlayer() {

    final var capturedMatchId = new AtomicReference<MatchId>();
    final var capturedPlayerId = new AtomicReference<PlayerId>();
    final var savedMatch = new AtomicReference<Match>();

    final MatchRepository repository = savedMatch::set;
    final PlayerTokenProvider tokenProvider = (matchId, playerId) -> {
      capturedMatchId.set(matchId);
      capturedPlayerId.set(playerId);
      return "token";
    };
    final var handler = new CreateMatchCommandHandler(repository, tokenProvider);

    handler.handle(new CreateMatchCommand(5));

    assertThat(savedMatch.get()).isNotNull();
    assertThat(capturedMatchId.get()).isEqualTo(savedMatch.get().getId());
    assertThat(capturedPlayerId.get()).isEqualTo(savedMatch.get().getPlayerOne());
  }

}
