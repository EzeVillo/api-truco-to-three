package com.villo.truco.domain.model.spectator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.InvalidMatchStateException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.spectator.exceptions.AlreadySpectatingException;
import com.villo.truco.domain.model.spectator.exceptions.CannotSpectateOwnMatchException;
import com.villo.truco.domain.model.spectator.exceptions.SpectateBotMatchNotOwnerException;
import com.villo.truco.domain.model.spectator.exceptions.SpectateNotAllowedException;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.testutil.InMemoryBotVsBotMatchRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpectatingEligibilityPolicy")
class SpectatingEligibilityPolicyTest {

  private static final InMemoryBotVsBotMatchRegistry NO_BOT_VS_BOT = new InMemoryBotVsBotMatchRegistry();

  private static Match startedMatch() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    match.startMatch(playerOne);
    match.startMatch(playerTwo);
    return match;
  }

  @Test
  @DisplayName("permite espectar por amistad aceptada aunque no pertenezca a liga o copa")
  void allowsAcceptedFriendship() {

    final var match = startedMatch();
    final var spectator = PlayerId.generate();
    final var policy = new SpectatingEligibilityPolicy((matchId, playerId) -> false,
        (m, playerId) -> playerId.equals(spectator), NO_BOT_VS_BOT);

    policy.ensureCanStartWatching(Spectatorship.create(spectator), match);
  }

  @Test
  @DisplayName("rechaza si no hay competencia ni amistad aceptada")
  void rejectsWithoutCompetitionOrFriendship() {

    final var policy = new SpectatingEligibilityPolicy((matchId, playerId) -> false,
        (match, spectatorId) -> false, NO_BOT_VS_BOT);

    assertThatThrownBy(
        () -> policy.ensureCanStartWatching(Spectatorship.create(PlayerId.generate()),
            startedMatch())).isInstanceOf(SpectateNotAllowedException.class);
  }

  @Test
  @DisplayName("rechaza jugador propio antes de evaluar amistad")
  void rejectsOwnMatchBeforeFriendship() {

    final var match = startedMatch();
    final var policy = new SpectatingEligibilityPolicy((matchId, playerId) -> true,
        (m, spectatorId) -> true, NO_BOT_VS_BOT);

    assertThatThrownBy(
        () -> policy.ensureCanStartWatching(Spectatorship.create(match.getPlayerOne()),
            match)).isInstanceOf(CannotSpectateOwnMatchException.class);
  }

  @Test
  @DisplayName("permite espectar el mismo match ya activo (multidispositivo)")
  void allowsSameMatchWhenAlreadyWatching() {

    final var match = startedMatch();
    final var spectator = PlayerId.generate();
    final var policy = new SpectatingEligibilityPolicy((matchId, playerId) -> true,
        (m, playerId) -> false, NO_BOT_VS_BOT);
    final var spectatorship = Spectatorship.create(spectator);
    spectatorship.startWatching(match.getId());

    assertThatCode(
        () -> policy.ensureCanStartWatching(spectatorship, match)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("rechaza si ya esta espectando otra partida")
  void rejectsAlreadySpectatingDifferentMatch() {

    final var match = startedMatch();
    final var spectator = PlayerId.generate();
    final var policy = new SpectatingEligibilityPolicy((matchId, playerId) -> true,
        (m, playerId) -> false, NO_BOT_VS_BOT);
    final var spectatorship = Spectatorship.create(spectator);
    spectatorship.startWatching(MatchId.generate());

    assertThatThrownBy(() -> policy.ensureCanStartWatching(spectatorship, match)).isInstanceOf(
        AlreadySpectatingException.class);
  }

  @Test
  @DisplayName("rechaza match que no esta en progreso")
  void rejectsMatchNotInProgress() {

    final var match = Match.createReady(PlayerId.generate(), PlayerId.generate(),
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), true));
    final var policy = new SpectatingEligibilityPolicy((matchId, playerId) -> true,
        (m, spectatorId) -> true, NO_BOT_VS_BOT);

    assertThatThrownBy(
        () -> policy.ensureCanStartWatching(Spectatorship.create(PlayerId.generate()),
            match)).isInstanceOf(InvalidMatchStateException.class);
  }

  @Test
  @DisplayName("bot-vs-bot: permite al dueño aunque no haya competencia ni amistad")
  void botVsBotAllowsOwner() {

    final var match = startedMatch();
    final var owner = PlayerId.generate();
    final var registry = new InMemoryBotVsBotMatchRegistry();
    registry.register(match.getId(), owner);
    final var policy = new SpectatingEligibilityPolicy((matchId, playerId) -> false,
        (m, spectatorId) -> false, registry);

    assertThatCode(() -> policy.ensureCanStartWatching(Spectatorship.create(owner),
        match)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("bot-vs-bot: rechaza a un no-dueño con SpectateBotMatchNotOwnerException")
  void botVsBotRejectsNonOwner() {

    final var match = startedMatch();
    final var owner = PlayerId.generate();
    final var registry = new InMemoryBotVsBotMatchRegistry();
    registry.register(match.getId(), owner);
    final var policy = new SpectatingEligibilityPolicy((matchId, playerId) -> true,
        (m, spectatorId) -> true, registry);

    assertThatThrownBy(
        () -> policy.ensureCanStartWatching(Spectatorship.create(PlayerId.generate()),
            match)).isInstanceOf(SpectateBotMatchNotOwnerException.class);
  }

  @Test
  @DisplayName("la rama de partidas con humanos sigue intacta cuando no es bot-vs-bot")
  void humanBranchRemainsIntact() {

    final var match = startedMatch();
    final var spectator = PlayerId.generate();
    final var policy = new SpectatingEligibilityPolicy((matchId, playerId) -> false,
        (m, playerId) -> playerId.equals(spectator), new InMemoryBotVsBotMatchRegistry());

    assertThatCode(() -> policy.ensureCanStartWatching(Spectatorship.create(spectator),
        match)).doesNotThrowAnyException();
  }

}
