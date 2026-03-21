package com.villo.truco.domain.model.cup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.cup.exceptions.BoutAlreadyResolvedException;
import com.villo.truco.domain.model.cup.exceptions.CupCreatorCannotLeaveException;
import com.villo.truco.domain.model.cup.exceptions.CupNotReadyException;
import com.villo.truco.domain.model.cup.exceptions.CupNotWaitingException;
import com.villo.truco.domain.model.cup.exceptions.InvalidCupPlayersException;
import com.villo.truco.domain.model.cup.exceptions.OnlyCreatorCanStartCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerAlreadyInCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerNotInCupException;
import com.villo.truco.domain.model.cup.valueobjects.BoutStatus;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CupTest {

  private static Cup createStartedCup(final PlayerId... players) {

    final var cup = Cup.create(players[0], players.length, GamesToPlay.of(3));
    for (int i = 1; i < players.length; i++) {
      cup.join(players[i], cup.getInviteCode());
    }
    cup.start(players[0]);
    return cup;
  }

  private static PlayerId[] generatePlayers(final int count) {

    final var players = new PlayerId[count];
    for (int i = 0; i < count; i++) {
      players[i] = PlayerId.generate();
    }
    return players;
  }

  private static List<Cup.BoutView> boutsInRound(final Cup cup, final int round) {

    return cup.getBouts().stream().filter(b -> b.roundNumber() == round).toList();
  }

  @Test
  @DisplayName("create: estado inicial WAITING_FOR_PLAYERS, creador como primer participante")
  void createInitialState() {

    final var creator = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(3));

    assertThat(cup.getStatus()).isEqualTo(CupStatus.WAITING_FOR_PLAYERS);
    assertThat(cup.getInviteCode()).isNotNull();
    assertThat(cup.getId()).isNotNull();
  }

  @Test
  @DisplayName("create: lanza InvalidCupPlayersException si numberOfPlayers < 4")
  void createRejectsFewerThan4Players() {

    assertThatThrownBy(() -> Cup.create(PlayerId.generate(), 3, GamesToPlay.of(1))).isInstanceOf(
        InvalidCupPlayersException.class);
  }

  @Test
  @DisplayName("create: lanza InvalidCupPlayersException si numberOfPlayers > 8")
  void createRejectsMoreThan8Players() {

    assertThatThrownBy(() -> Cup.create(PlayerId.generate(), 9, GamesToPlay.of(1))).isInstanceOf(
        InvalidCupPlayersException.class);
  }

  @Test
  @DisplayName("join: transiciona a WAITING_FOR_START cuando se llena")
  void joinTransitionsToWaitingForStartWhenFull() {

    final var cup = Cup.create(PlayerId.generate(), 4, GamesToPlay.of(1));
    cup.join(PlayerId.generate(), cup.getInviteCode());
    cup.join(PlayerId.generate(), cup.getInviteCode());
    cup.join(PlayerId.generate(), cup.getInviteCode());

    assertThat(cup.getStatus()).isEqualTo(CupStatus.WAITING_FOR_START);
  }

  @Test
  @DisplayName("join: lanza CupNotWaitingException cuando la copa ya está llena (estado WAITING_FOR_START)")
  void joinRejectsWhenFull() {

    final var cup = Cup.create(PlayerId.generate(), 4, GamesToPlay.of(1));
    cup.join(PlayerId.generate(), cup.getInviteCode());
    cup.join(PlayerId.generate(), cup.getInviteCode());
    cup.join(PlayerId.generate(), cup.getInviteCode());

    assertThatThrownBy(() -> cup.join(PlayerId.generate(), cup.getInviteCode())).isInstanceOf(
        CupNotWaitingException.class);
  }

  @Test
  @DisplayName("join: lanza PlayerAlreadyInCupException si el jugador ya está")
  void joinRejectsDuplicatePlayer() {

    final var creator = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(1));

    assertThatThrownBy(() -> cup.join(creator, cup.getInviteCode())).isInstanceOf(
        PlayerAlreadyInCupException.class);
  }

  @Test
  @DisplayName("leave: el creador no puede salir")
  void leaveRejectsCreator() {

    final var creator = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(1));
    cup.join(PlayerId.generate(), cup.getInviteCode());

    assertThatThrownBy(() -> cup.leave(creator)).isInstanceOf(CupCreatorCannotLeaveException.class);
  }

  @Test
  @DisplayName("leave: vuelve a WAITING_FOR_PLAYERS al salir de WAITING_FOR_START")
  void leaveFromWaitingForStartTransitionsBack() {

    final var players = generatePlayers(4);
    final var cup = Cup.create(players[0], 4, GamesToPlay.of(1));
    for (int i = 1; i < 4; i++) {
      cup.join(players[i], cup.getInviteCode());
    }
    assertThat(cup.getStatus()).isEqualTo(CupStatus.WAITING_FOR_START);

    cup.leave(players[3]);

    assertThat(cup.getStatus()).isEqualTo(CupStatus.WAITING_FOR_PLAYERS);
  }

  @Test
  @DisplayName("start: lanza OnlyCreatorCanStartCupException si no es el creador")
  void startRejectsNonCreator() {

    final var players = generatePlayers(4);
    final var cup = Cup.create(players[0], 4, GamesToPlay.of(1));
    for (int i = 1; i < 4; i++) {
      cup.join(players[i], cup.getInviteCode());
    }

    assertThatThrownBy(() -> cup.start(players[1])).isInstanceOf(
        OnlyCreatorCanStartCupException.class);
  }

  @Test
  @DisplayName("start: lanza CupNotReadyException si el estado no es WAITING_FOR_START")
  void startRejectsWhenNotReady() {

    final var creator = PlayerId.generate();
    final var cup = Cup.create(creator, 4, GamesToPlay.of(1));

    assertThatThrownBy(() -> cup.start(creator)).isInstanceOf(CupNotReadyException.class);
  }

  @Test
  @DisplayName("bracket 4 jugadores: 2 PENDING en R1, 1 AWAITING en R2 (Final)")
  void bracketForFourPlayers() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    assertThat(cup.getStatus()).isEqualTo(CupStatus.IN_PROGRESS);

    final var r1Bouts = boutsInRound(cup, 1);
    final var r2Bouts = boutsInRound(cup, 2);

    assertThat(r1Bouts).hasSize(2);
    assertThat(r2Bouts).hasSize(1);

    assertThat(r1Bouts).allMatch(b -> b.status() == BoutStatus.PENDING);
    assertThat(r2Bouts).allMatch(b -> b.status() == BoutStatus.AWAITING);

    final var r1Players = r1Bouts.stream()
        .flatMap(b -> Set.of(b.playerOne(), b.playerTwo()).stream()).collect(Collectors.toSet());
    assertThat(r1Players).hasSize(4);
  }

  @Test
  @DisplayName("bracket 5 jugadores: 1 PENDING, 3 BYE en R1; bye-winners se distribuyen en R2")
  void bracketForFivePlayers() {

    final var players = generatePlayers(5);
    final var cup = createStartedCup(players);

    final var r1Bouts = boutsInRound(cup, 1);
    assertThat(r1Bouts).hasSize(4);
    assertThat(r1Bouts.stream().filter(b -> b.status() == BoutStatus.PENDING).count()).isEqualTo(1);
    assertThat(r1Bouts.stream().filter(b -> b.status() == BoutStatus.BYE).count()).isEqualTo(3);

    final var r2Bouts = boutsInRound(cup, 2);
    assertThat(r2Bouts).hasSize(2);
    assertThat(r2Bouts.stream().filter(b -> b.playerOne() != null || b.playerTwo() != null)
        .count()).isEqualTo(2);
    assertThat(r2Bouts.stream().filter(b -> b.status() == BoutStatus.PENDING).count()).isEqualTo(1);
    assertThat(r2Bouts.stream().filter(b -> b.status() == BoutStatus.AWAITING).count()).isEqualTo(
        1);
  }

  @Test
  @DisplayName("bracket 6 jugadores: 2 PENDING, 2 BYE en R1; R2 tiene 2 jugadores pre-asignados")
  void bracketForSixPlayers() {

    final var players = generatePlayers(6);
    final var cup = createStartedCup(players);

    final var r1Bouts = boutsInRound(cup, 1);
    assertThat(r1Bouts.stream().filter(b -> b.status() == BoutStatus.PENDING).count()).isEqualTo(2);
    assertThat(r1Bouts.stream().filter(b -> b.status() == BoutStatus.BYE).count()).isEqualTo(2);
  }

  @Test
  @DisplayName("bracket 8 jugadores: todos PENDING en R1, sin byes")
  void bracketForEightPlayers() {

    final var players = generatePlayers(8);
    final var cup = createStartedCup(players);

    final var r1Bouts = boutsInRound(cup, 1);
    assertThat(r1Bouts).hasSize(4);
    assertThat(r1Bouts).allMatch(b -> b.status() == BoutStatus.PENDING);
    assertThat(r1Bouts.stream().noneMatch(b -> b.status() == BoutStatus.BYE)).isTrue();

    final var r2Bouts = boutsInRound(cup, 2);
    assertThat(r2Bouts).hasSize(2);
    assertThat(r2Bouts).allMatch(b -> b.status() == BoutStatus.AWAITING);
  }

  @Test
  @DisplayName("bracket 7 jugadores: 3 PENDING, 1 BYE en R1")
  void bracketForSevenPlayers() {

    final var players = generatePlayers(7);
    final var cup = createStartedCup(players);

    final var r1Bouts = boutsInRound(cup, 1);
    assertThat(r1Bouts.stream().filter(b -> b.status() == BoutStatus.PENDING).count()).isEqualTo(3);
    assertThat(r1Bouts.stream().filter(b -> b.status() == BoutStatus.BYE).count()).isEqualTo(1);
  }

  @Test
  @DisplayName("recordMatchWinner: avanza al ganador al siguiente bout (4p)")
  void recordMatchWinnerAdvancesToNextRound() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    final var r1Bouts = boutsInRound(cup, 1);
    final var firstBout = r1Bouts.getFirst();
    final var matchId = MatchId.generate();
    cup.linkBoutMatch(firstBout.boutId(), matchId);

    final var winner = firstBout.playerOne();
    final var result = cup.recordMatchWinner(matchId, winner);

    assertThat(result.cupFinished()).isFalse();
    assertThat(result.pendingPairings()).isEmpty();

    final var r2Bout = boutsInRound(cup, 2).getFirst();
    final var winnerInR2 = winner.equals(r2Bout.playerOne()) || winner.equals(r2Bout.playerTwo());
    assertThat(winnerInR2).isTrue();
  }

  @Test
  @DisplayName("recordMatchWinner: copa termina tras la final")
  void recordMatchWinnerFinishesCupOnFinal() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    final var r1Bouts = boutsInRound(cup, 1);

    final var m1 = MatchId.generate();
    final var m2 = MatchId.generate();
    cup.linkBoutMatch(r1Bouts.get(0).boutId(), m1);
    cup.linkBoutMatch(r1Bouts.get(1).boutId(), m2);

    final var w1 = r1Bouts.get(0).playerOne();
    final var w2 = r1Bouts.get(1).playerOne();
    cup.recordMatchWinner(m1, w1);
    cup.recordMatchWinner(m2, w2);

    final var r2Bout = boutsInRound(cup, 2).getFirst();
    assertThat(r2Bout.status()).isEqualTo(BoutStatus.PENDING);

    final var finalMatchId = MatchId.generate();
    cup.linkBoutMatch(r2Bout.boutId(), finalMatchId);

    final var champion = r2Bout.playerOne();
    final var finalResult = cup.recordMatchWinner(finalMatchId, champion);

    assertThat(finalResult.cupFinished()).isTrue();
    assertThat(finalResult.champion()).isEqualTo(champion);
    assertThat(cup.getStatus()).isEqualTo(CupStatus.FINISHED);
    assertThat(cup.getChampion()).isEqualTo(champion);
  }

  @Test
  @DisplayName("recordMatchWinner: idempotente con el mismo winner ya registrado")
  void recordMatchWinnerIdempotentWithSameWinner() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    final var firstBout = boutsInRound(cup, 1).getFirst();
    final var matchId = MatchId.generate();
    cup.linkBoutMatch(firstBout.boutId(), matchId);

    final var winner = firstBout.playerOne();
    cup.recordMatchWinner(matchId, winner);

    final var secondResult = cup.recordMatchWinner(matchId, winner);
    assertThat(secondResult.pendingPairings()).isEmpty();
    assertThat(secondResult.cupFinished()).isFalse();
  }

  @Test
  @DisplayName("recordMatchWinner: lanza BoutAlreadyResolvedException con diferente winner")
  void recordMatchWinnerThrowsWhenBoutAlreadyResolvedDifferentWinner() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    final var firstBout = boutsInRound(cup, 1).getFirst();
    final var matchId = MatchId.generate();
    cup.linkBoutMatch(firstBout.boutId(), matchId);

    cup.recordMatchWinner(matchId, firstBout.playerOne());

    assertThatThrownBy(() -> cup.recordMatchWinner(matchId, firstBout.playerTwo())).isInstanceOf(
        BoutAlreadyResolvedException.class);
  }

  @Test
  @DisplayName("forfeitPlayer: auto-resuelve el bout PENDING del jugador")
  void forfeitPlayerAutoResolvesPendingBout() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    final var firstBout = boutsInRound(cup, 1).getFirst();
    final var forfeiter = firstBout.playerOne();
    final var opponent = firstBout.playerTwo();

    cup.forfeitPlayer(forfeiter);

    final var updatedBout = boutsInRound(cup, 1).getFirst();
    assertThat(updatedBout.status()).isEqualTo(BoutStatus.FINISHED);
    assertThat(updatedBout.winner()).isEqualTo(opponent);
  }

  @Test
  @DisplayName("forfeitPlayer: lanza PlayerNotInCupException si el jugador no está en la copa")
  void forfeitPlayerThrowsWhenPlayerNotInCup() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    assertThatThrownBy(() -> cup.forfeitPlayer(PlayerId.generate())).isInstanceOf(
        PlayerNotInCupException.class);
  }

  @Test
  @DisplayName("forfeitPlayer: forfeit en R1 auto-resuelve el bout y avanza al oponente a la final")
  void forfeitCascadeResolvesTransitively() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    final var r1Bouts = boutsInRound(cup, 1);
    final var bout0 = r1Bouts.get(0);
    final var bout1 = r1Bouts.get(1);

    final var m1 = MatchId.generate();
    cup.linkBoutMatch(bout0.boutId(), m1);
    final var w1 = bout0.playerOne();
    cup.recordMatchWinner(m1, w1);

    final var forfeiter = bout1.playerOne();
    cup.forfeitPlayer(forfeiter);

    final var r2Bout = boutsInRound(cup, 2).getFirst();
    assertThat(r2Bout.status()).isEqualTo(BoutStatus.PENDING);
    assertThat(List.of(r2Bout.playerOne(), r2Bout.playerTwo())).contains(w1);
    assertThat(List.of(r2Bout.playerOne(), r2Bout.playerTwo())).contains(bout1.playerTwo());
  }

  @Test
  @DisplayName("linkBoutMatch: idempotente con el mismo matchId")
  void linkBoutMatchIdempotentSameMatchId() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    final var firstBout = boutsInRound(cup, 1).getFirst();
    final var matchId = MatchId.generate();
    cup.linkBoutMatch(firstBout.boutId(), matchId);
    cup.linkBoutMatch(firstBout.boutId(), matchId);
  }

  @Test
  @DisplayName("getRounds: devuelve rondas ordenadas con el número correcto de bouts (4p)")
  void getRoundsStructureForFourPlayers() {

    final var players = generatePlayers(4);
    final var cup = createStartedCup(players);

    final var rounds = cup.getRounds();
    assertThat(rounds).hasSize(2);
    assertThat(rounds.get(0).roundNumber()).isEqualTo(1);
    assertThat(rounds.get(0).bouts()).hasSize(2);
    assertThat(rounds.get(1).roundNumber()).isEqualTo(2);
    assertThat(rounds.get(1).bouts()).hasSize(1);
  }

}
