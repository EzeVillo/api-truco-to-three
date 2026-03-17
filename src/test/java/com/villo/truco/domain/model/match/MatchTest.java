package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.match.events.GameScoreChangedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.exceptions.InvalidInviteCodeException;
import com.villo.truco.domain.model.match.exceptions.InvalidMatchStateException;
import com.villo.truco.domain.model.match.exceptions.MatchNotFullException;
import com.villo.truco.domain.model.match.exceptions.NotYourTurnException;
import com.villo.truco.domain.model.match.exceptions.PlayerNotInMatchException;
import com.villo.truco.domain.model.match.exceptions.SamePlayerMatchException;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatchTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
  }

  // ===== CREACIÓN =====

  private Match matchInProgress() {

    final var match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
    match.startMatch(playerOne);
    match.startMatch(playerTwo);
    return match;
  }

  // ===== JOIN =====

  // termina el juego actual acumulando 3 puntos para el ganador via truco rechazado
  // quien tenga el turno canta, el otro rechaza — siempre gana 1 punto quien cantó
  private void finishGame(final Match match, final PlayerId winner) {

    final var loser = winner.equals(playerOne) ? playerTwo : playerOne;
    final var winnerGamesBefore =
        winner.equals(playerOne) ? match.getGamesWonPlayerOne() : match.getGamesWonPlayerTwo();

    while ((winner.equals(playerOne) ? match.getGamesWonPlayerOne() : match.getGamesWonPlayerTwo())
        == winnerGamesBefore) {
      final var currentTurn = match.getCurrentTurn();
      if (currentTurn.equals(winner)) {
        match.callTruco(winner);
        match.rejectTruco(loser);
      } else {
        match.playCard(loser, match.getCurrentRound().getHandOf(loser).getCards().getFirst());
      }
    }
  }

  // ===== TURNOS =====

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("crea el match en WAITING_FOR_PLAYERS")
    void createsMatchInWaitingState() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));

      assertThat(match.getStatus()).isEqualTo(MatchStatus.WAITING_FOR_PLAYERS);
    }

    @Test
    @DisplayName("inicializa juegos ganados en 0")
    void initializesWinsAtZero() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));

      assertThat(match.getGamesWonPlayerOne()).isZero();
      assertThat(match.getGamesWonPlayerTwo()).isZero();
    }

    @Test
    @DisplayName("no tiene ganador al crearse")
    void hasNoWinnerOnCreation() {

      assertThat(Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)))
          .getMatchWinner()).isNull();
    }

    @Test
    @DisplayName("permite crear match con reglas custom")
    void createsMatchWithCustomRules() {

      final var match = Match.create(playerOne, new MatchRules(1));
      match.join(playerTwo, match.getInviteCode());
      match.startMatch(playerOne);
      match.startMatch(playerTwo);

      finishGame(match, playerOne);

      assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
      assertThat(match.getMatchWinner()).isEqualTo(playerOne);
    }

  }

  // ===== TRUCO =====

  @Nested
  @DisplayName("join")
  class Join {

    @Test
    @DisplayName("no arranca el juego todavía — no hay turno asignado")
    void doesNotStartGame() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
      match.join(playerTwo, match.getInviteCode());

      assertThat(match.getCurrentTurn()).isNull();
    }

    @Test
    @DisplayName("transiciona el estado a READY")
    void transitionsToReady() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
      match.join(playerTwo, match.getInviteCode());

      assertThat(match.getStatus()).isEqualTo(MatchStatus.READY);
    }

    @Test
    @DisplayName("falla si el mismo jugador intenta hacer join")
    void failsIfSamePlayer() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
      assertThatThrownBy(() -> match.join(playerOne, match.getInviteCode())).isInstanceOf(
          SamePlayerMatchException.class);
    }

    @Test
    @DisplayName("falla si el inviteCode no coincide con el esperado")
    void failsIfWrongInviteCode() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));

      assertThatThrownBy(() -> match.join(playerTwo, InviteCode.generate())).isInstanceOf(
          InvalidInviteCodeException.class);
    }

  }

  @Nested
  @DisplayName("startMatch")
  class StartMatch {

    @Test
    @DisplayName("lanza MatchNotFullException si no se hizo join antes")
    void failsIfNoJoin() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(3)));

      assertThatThrownBy(() -> match.startMatch(playerOne)).isInstanceOf(
          MatchNotFullException.class);
    }

    @Test
    @DisplayName("un solo jugador ready no inicia el match")
    void singleReadyDoesNotStart() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
      match.join(playerTwo, match.getInviteCode());
      match.startMatch(playerOne);

      assertThat(match.getStatus()).isNotEqualTo(MatchStatus.IN_PROGRESS);
      assertThat(match.isReadyPlayerOne()).isTrue();
      assertThat(match.isReadyPlayerTwo()).isFalse();
      assertThat(match.getCurrentTurn()).isNull();
    }

    @Test
    @DisplayName("ambos jugadores ready inicia el match")
    void bothReadyStartsMatch() {

      final var match = matchInProgress();

      assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
      assertThat(match.getCurrentTurn()).isNotNull();
    }

    @Test
    @DisplayName("playerOne es mano en el primer juego")
    void playerOneIsManoInFirstGame() {

      final var match = matchInProgress();

      assertThat(match.getCurrentTurn()).isEqualTo(playerOne);
    }

    @Test
    @DisplayName("startMatch es idempotente")
    void startMatchIsIdempotent() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
      match.join(playerTwo, match.getInviteCode());
      match.startMatch(playerOne);
      match.startMatch(playerOne);

      assertThat(match.getStatus()).isNotEqualTo(MatchStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("startMatch es idempotente cuando ya está IN_PROGRESS")
    void startMatchIsIdempotentWhenAlreadyInProgress() {

      final var match = matchInProgress();

      assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);

      match.startMatch(playerOne);
      match.startMatch(playerTwo);

      assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    }

  }

  // ===== ENVIDO =====

  @Nested
  @DisplayName("turnos")
  class Turnos {

    @Test
    @DisplayName("falla si jugás fuera de turno")
    void failsWhenPlayingOutOfTurn() {

      final var match = matchInProgress();
      final var card = match.getCurrentRound().getHandOf(playerTwo).getCards().getFirst();

      assertThatThrownBy(() -> match.playCard(playerTwo, card)).isInstanceOf(
          NotYourTurnException.class);
    }

    @Test
    @DisplayName("falla si un jugador ajeno intenta jugar")
    void failsWhenStrangerPlays() {

      final var match = matchInProgress();
      final var stranger = PlayerId.generate();
      final var card = match.getCurrentRound().getHandOf(playerOne).getCards().getFirst();

      assertThatThrownBy(() -> match.playCard(stranger, card)).isInstanceOf(
          PlayerNotInMatchException.class);
    }

  }

  // ===== ALTERNANCIA DEL MANO ENTRE RONDAS =====

  @Nested
  @DisplayName("truco")
  class Truco {

    @Test
    @DisplayName("rechazar truco da 1 punto al que cantó")
    void rejectTrucoGivesOnePoint() {

      final var match = matchInProgress();

      match.callTruco(playerOne);
      match.rejectTruco(playerTwo);

      assertThat(match.getScorePlayerOne()).isEqualTo(1);
      assertThat(match.getScorePlayerTwo()).isZero();
    }

    @Test
    @DisplayName("aceptar y irse al mazo da 2 puntos al que cantó")
    void acceptAndFoldGivesTwoPoints() {

      final var match = matchInProgress();

      match.callTruco(playerOne);
      match.acceptTrucoAndFold(playerTwo);

      assertThat(match.getScorePlayerOne()).isEqualTo(2);
    }

    @Test
    @DisplayName("el juego termina al llegar a 3 puntos")
    void gameFinishesAtThreePoints() {

      final var match = matchInProgress();

      // playerOne siempre canta truco — si no tiene el turno, juega una carta primero
      for (int i = 0; i < 3; i++) {
        if (!match.getCurrentTurn().equals(playerOne)) {
          final var card = match.getCurrentRound().getHandOf(playerTwo).getCards().getFirst();
          match.playCard(playerTwo, card);
        }
        match.callTruco(playerOne);
        match.rejectTruco(playerTwo);
      }

      assertThat(match.getGamesWonPlayerOne()).isEqualTo(1);
    }

    @Test
    @DisplayName("pasarse de 3 termina el juego")
    void exceedingThreeFinishesGame() {

      final var match = matchInProgress();

      match.callTruco(playerOne);
      match.acceptTrucoAndFold(playerTwo);
      match.playCard(playerTwo, match.getCurrentRound().getHandOf(playerTwo).getCards().getFirst());
      match.callTruco(playerOne);
      match.acceptTrucoAndFold(playerTwo);

      assertThat(match.getGamesWonPlayerTwo()).isEqualTo(1);
      assertThat(match.getGamesWonPlayerOne()).isZero();
    }

    @Test
    @DisplayName("falla si cantás truco fuera de turno")
    void failsWhenCallingTrucoOutOfTurn() {

      final var match = matchInProgress();

      assertThatThrownBy(() -> match.callTruco(playerTwo)).isInstanceOf(NotYourTurnException.class);
    }

  }

  // ===== ALTERNANCIA DEL MANO ENTRE JUEGOS =====

  @Nested
  @DisplayName("envido")
  class Envido {

    @Test
    @DisplayName("rechazar envido da 1 punto al que cantó")
    void rejectEnvidoGivesOnePoint() {

      final var match = matchInProgress();

      match.callEnvido(playerOne, EnvidoCall.ENVIDO);
      match.rejectEnvido(playerTwo);

      assertThat(match.getScorePlayerOne()).isEqualTo(1);
      assertThat(match.getScorePlayerTwo()).isZero();
    }

    @Test
    @DisplayName("rechazar cadena da la suma menos el último")
    void rejectChainGivesSumMinusLast() {

      final var match = matchInProgress();

      match.callEnvido(playerOne, EnvidoCall.ENVIDO);
      match.callEnvido(playerTwo, EnvidoCall.REAL_ENVIDO);
      match.rejectEnvido(playerOne);

      assertThat(match.getScorePlayerOne()).isZero();
      assertThat(match.getScorePlayerTwo()).isEqualTo(2);
    }

    @Test
    @DisplayName("el juego puede terminar por envido")
    void gameCanFinishByEnvido() {

      final var match = matchInProgress();

      // playerOne siempre canta envido — si no tiene el turno, juega una carta primero
      // después de cada envido resuelto, terminar la ronda con fold para ir a la siguiente
      for (int i = 0; i < 3; i++) {
        if (!match.getCurrentTurn().equals(playerOne)) {
          final var card = match.getCurrentRound().getHandOf(playerTwo).getCards().getFirst();
          match.playCard(playerTwo, card);
        }
        match.callEnvido(playerOne, EnvidoCall.ENVIDO);
        match.rejectEnvido(playerTwo);

        if (match.getGamesWonPlayerOne() < 1) {
          match.fold(match.getCurrentTurn());
        }
      }

      assertThat(match.getGamesWonPlayerOne()).isEqualTo(1);
    }

  }

  // ===== SERIE (AL MEJOR DE 5) =====

  @Nested
  @DisplayName("alternancia del mano entre rondas")
  class AlternanciaRondas {

    @Test
    @DisplayName("playerTwo es mano en la segunda ronda del juego")
    void playerTwoIsManoInSecondRound() {

      final var match = matchInProgress();

      // terminar la primera ronda
      match.callTruco(playerOne);
      match.rejectTruco(playerTwo);

      assertThat(match.getCurrentTurn()).isEqualTo(playerTwo);
    }

    @Test
    @DisplayName("playerOne vuelve a ser mano en la tercera ronda")
    void playerOneIsManoAgainInThirdRound() {

      final var match = matchInProgress();

      match.callTruco(playerOne);
      match.rejectTruco(playerTwo);

      match.callTruco(playerTwo);
      match.rejectTruco(playerOne);

      assertThat(match.getCurrentTurn()).isEqualTo(playerOne);
    }

  }

  // ===== HELPERS =====

  @Nested
  @DisplayName("alternancia del mano entre juegos")
  class AlternanciaJuegos {

    @Test
    @DisplayName("playerTwo es mano en el segundo juego")
    void playerTwoIsManoInSecondGame() {

      final var match = matchInProgress();

      finishGame(match, playerOne);

      // el segundo juego arrancó — playerTwo es mano
      assertThat(match.getCurrentTurn()).isEqualTo(playerTwo);
    }

    @Test
    @DisplayName("playerOne vuelve a ser mano en el tercer juego")
    void playerOneIsManoAgainInThirdGame() {

      final var match = matchInProgress();

      finishGame(match, playerOne);
      finishGame(match, playerTwo);

      assertThat(match.getCurrentTurn()).isEqualTo(playerOne);
    }

    @Test
    @DisplayName("la alternancia es independiente de quién ganó el juego")
    void alternationIsIndependentOfWinner() {

      final var match = matchInProgress();

      // playerTwo gana el primer juego — igual playerTwo es mano en el segundo
      finishGame(match, playerTwo);

      assertThat(match.getCurrentTurn()).isEqualTo(playerTwo);
    }

  }

  @Nested
  @DisplayName("serie al mejor de 5")
  class Serie {

    @Test
    @DisplayName("el match termina cuando playerOne gana 3 juegos")
    void matchFinishesWhenPlayerOneWins3Games() {

      final var match = matchInProgress();

      finishGame(match, playerOne);
      finishGame(match, playerOne);
      finishGame(match, playerOne);

      assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
      assertThat(match.getMatchWinner()).isEqualTo(playerOne);
    }

    @Test
    @DisplayName("el match termina cuando playerTwo gana 3 juegos")
    void matchFinishesWhenPlayerTwoWins3Games() {

      final var match = matchInProgress();

      finishGame(match, playerTwo);
      finishGame(match, playerTwo);
      finishGame(match, playerTwo);

      assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
      assertThat(match.getMatchWinner()).isEqualTo(playerTwo);
    }

    @Test
    @DisplayName("el match no termina antes de 3 juegos ganados")
    void matchDoesNotFinishBefore3Wins() {

      final var match = matchInProgress();

      finishGame(match, playerOne);
      finishGame(match, playerOne);

      assertThat(match.isFinished()).isFalse();
      assertThat(match.getMatchWinner()).isNull();
    }

    @Test
    @DisplayName("score final correcto — ejemplo 3-2")
    void finalScoreIsCorrect() {

      final var match = matchInProgress();

      finishGame(match, playerOne);
      finishGame(match, playerOne);
      finishGame(match, playerTwo);
      finishGame(match, playerTwo);
      finishGame(match, playerOne);

      assertThat(match.getGamesWonPlayerOne()).isEqualTo(3);
      assertThat(match.getGamesWonPlayerTwo()).isEqualTo(2);
    }

    @Test
    @DisplayName("el score del juego se resetea al empezar un nuevo juego")
    void gameScoreResetsOnNewGame() {

      final var match = matchInProgress();

      finishGame(match, playerOne);

      // el score del nuevo juego debe ser 0-0
      assertThat(match.getScorePlayerOne()).isZero();
      assertThat(match.getScorePlayerTwo()).isZero();
    }

    @Test
    @DisplayName("emite GAME_SCORE_CHANGED cuando cambia la serie")
    void emitsGameScoreChangedWhenSeriesScoreChanges() {

      final var match = matchInProgress();
      match.clearDomainEvents();

      finishGame(match, playerOne);

      assertThat(match.getDomainEvents()).anyMatch(
          event -> event instanceof GameScoreChangedEvent gameScoreChangedEvent
              && gameScoreChangedEvent.getGamesWonPlayerOne() == 1
              && gameScoreChangedEvent.getGamesWonPlayerTwo() == 0);
    }

  }

  @Nested
  @DisplayName("forfeit")
  class Forfeit {

    @Test
    @DisplayName("playerOne gana cuando playerTwo hace forfeit")
    void playerOneWinsWhenPlayerTwoForfeits() {

      final var match = matchInProgress();

      match.forfeit(playerOne);

      assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
      assertThat(match.getMatchWinner()).isEqualTo(playerOne);
    }

    @Test
    @DisplayName("playerTwo gana cuando playerOne hace forfeit")
    void playerTwoWinsWhenPlayerOneForfeits() {

      final var match = matchInProgress();

      match.forfeit(playerTwo);

      assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
      assertThat(match.getMatchWinner()).isEqualTo(playerTwo);
    }

    @Test
    @DisplayName("emite MatchForfeitedEvent con el seat del ganador correcto")
    void emitsMatchForfeitedEventWithCorrectWinnerSeat() {

      final var match = matchInProgress();
      match.clearDomainEvents();

      match.forfeit(playerTwo);

      assertThat(match.getDomainEvents()).anyMatch(
          event -> event instanceof MatchForfeitedEvent forfeited
              && forfeited.getWinnerSeat() == PlayerSeat.PLAYER_TWO);
    }

    @Test
    @DisplayName("forfeit en WAITING_FOR_PLAYERS lanza InvalidMatchStateException")
    void forfeitInWaitingForPlayersThrows() {

      final var match = Match.create(playerOne, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));

      assertThatThrownBy(() -> match.forfeit(playerOne)).isInstanceOf(
          InvalidMatchStateException.class);
    }

    @Test
    @DisplayName("forfeit cuando ya está FINISHED es idempotente")
    void forfeitWhenAlreadyFinishedIsIdempotent() {

      final var match = matchInProgress();
      match.forfeit(playerOne);
      match.clearDomainEvents();

      match.forfeit(playerOne);

      assertThat(match.getDomainEvents()).isEmpty();
      assertThat(match.getMatchWinner()).isEqualTo(playerOne);
    }

    @Test
    @DisplayName("forfeit con jugador ajeno lanza PlayerNotInMatchException")
    void forfeitWithStrangerThrows() {

      final var match = matchInProgress();
      final var stranger = PlayerId.generate();

      assertThatThrownBy(() -> match.forfeit(stranger)).isInstanceOf(
          PlayerNotInMatchException.class);
    }

  }

}