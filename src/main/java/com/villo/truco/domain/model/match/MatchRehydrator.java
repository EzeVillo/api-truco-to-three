package com.villo.truco.domain.model.match;

import java.util.ArrayList;

public final class MatchRehydrator {

  private MatchRehydrator() {

  }

  public static Match rehydrate(final MatchSnapshot snapshot) {

    final Round currentRound =
        snapshot.currentRound() != null ? rehydrateRound(snapshot.currentRound()) : null;

    return Match.reconstruct(snapshot.id(), snapshot.playerOne(), snapshot.playerTwo(),
        snapshot.inviteCode(), snapshot.rules(), snapshot.status(), snapshot.gamesWonPlayerOne(),
        snapshot.gamesWonPlayerTwo(), snapshot.gameNumber(), snapshot.scorePlayerOne(),
        snapshot.scorePlayerTwo(), snapshot.roundNumber(), snapshot.readyPlayerOne(),
        snapshot.readyPlayerTwo(), snapshot.firstManoOfGame(), currentRound);
  }

  private static Round rehydrateRound(final RoundSnapshot roundSnapshot) {

    final var handPlayerOne = Hand.reconstruct(roundSnapshot.handPlayerOne().id(),
        new ArrayList<>(roundSnapshot.handPlayerOne().cards()));

    final var handPlayerTwo = Hand.reconstruct(roundSnapshot.handPlayerTwo().id(),
        new ArrayList<>(roundSnapshot.handPlayerTwo().cards()));

    final var playedHands = roundSnapshot.playedHands().stream()
        .map(ph -> new Round.PlayedHand(ph.cardMano(), ph.cardPie(), ph.winner())).toList();

    final var currentHandCards = roundSnapshot.currentHandCards().stream()
        .map(cp -> new Round.CardPlay(cp.playerId(), cp.card())).toList();

    final var round = Round.reconstruct(roundSnapshot.id(), roundSnapshot.roundNumber(),
        roundSnapshot.mano(), roundSnapshot.playerOne(), roundSnapshot.playerTwo(), handPlayerOne,
        handPlayerTwo, playedHands, currentHandCards, roundSnapshot.status(),
        roundSnapshot.currentTurn(), roundSnapshot.turnBeforeTrucoCall(),
        roundSnapshot.turnBeforeEnvidoCall());

    round.getTrucoStateMachine().initializeState(roundSnapshot.trucoStateMachine().currentCall(),
        roundSnapshot.trucoStateMachine().caller(),
        roundSnapshot.trucoStateMachine().pointsAtStake());

    round.getEnvidoStateMachine()
        .initializeState(new ArrayList<>(roundSnapshot.envidoStateMachine().chain()),
            roundSnapshot.envidoStateMachine().resolved());

    return round;
  }

}
