package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
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
    final var handler = new CreateMatchCommandHandler(repository);

    handler.handle(new CreateMatchCommand(PlayerId.generate().value().toString(), 3));

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getId()).isNotNull();
    assertThat(savedMatch.get().getInviteCode()).isNotNull();
  }

  @Test
  @DisplayName("falla si gamesToPlay no es 1, 3 o 5")
  void failsWhenGamesToPlayIsInvalid() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final var handler = new CreateMatchCommandHandler(repository);

    assertThatThrownBy(() -> handler.handle(
        new CreateMatchCommand(PlayerId.generate().value().toString(), 7))).isInstanceOf(
        DomainException.class);

    assertThat(savedMatch.get()).isNull();
  }

  @Test
  @DisplayName("el match creado pertenece al jugador del comando")
  void matchBelongsToCommandPlayer() {

    final var savedMatch = new AtomicReference<Match>();
    final MatchRepository repository = savedMatch::set;
    final var handler = new CreateMatchCommandHandler(repository);
    final var playerId = PlayerId.generate();

    handler.handle(new CreateMatchCommand(playerId.value().toString(), 5));

    assertThat(savedMatch.get()).isNotNull();
    assertThat(savedMatch.get().getPlayerOne()).isEqualTo(playerId);
  }

}
