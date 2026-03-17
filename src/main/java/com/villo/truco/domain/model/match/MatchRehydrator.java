package com.villo.truco.domain.model.match;

import java.util.ArrayList;

public final class MatchRehydrator {

  private MatchRehydrator() {

  }

  public static Match rehydrate(final MatchSnapshot.MatchData snapshot) {

    final Round currentRound =
        snapshot.currentRound() != null ? rehydrateRound(snapshot.currentRound()) : null;

    return Match.reconstruct(snapshot.id(), snapshot.playerOne(), snapshot.playerTwo(),
        snapshot.inviteCode(), snapshot.rules(), snapshot.status(), snapshot.gamesWonPlayerOne(),
        snapshot.gamesWonPlayerTwo(), snapshot.gameNumber(), snapshot.scorePlayerOne(),
        snapshot.scorePlayerTwo(), snapshot.roundNumber(), snapshot.readyPlayerOne(),
        snapshot.readyPlayerTwo(), snapshot.firstManoOfGame(), currentRound);
  }

  private static Round rehydrateRound(final MatchSnapshot.RoundData roundData) {

    final var handPlayerOne = Hand.reconstruct(roundData.handPlayerOne().id(),
        new ArrayList<>(roundData.handPlayerOne().cards()));

    final var handPlayerTwo = Hand.reconstruct(roundData.handPlayerTwo().id(),
        new ArrayList<>(roundData.handPlayerTwo().cards()));

    final var playedHands = roundData.playedHands().stream()
        .map(ph -> new Round.PlayedHand(ph.cardMano(), ph.cardPie(), ph.winner())).toList();

    final var currentHandCards = roundData.currentHandCards().stream()
        .map(cp -> new Round.CardPlay(cp.playerId(), cp.card())).toList();

    final var round = Round.reconstruct(roundData.id(), roundData.roundNumber(), roundData.mano(),
        roundData.playerOne(), roundData.playerTwo(), handPlayerOne, handPlayerTwo, playedHands,
        currentHandCards, roundData.status(), roundData.currentTurn(),
        roundData.turnBeforeTrucoCall(), roundData.turnBeforeEnvidoCall());

    round.getTrucoStateMachine().initializeState(roundData.trucoStateMachine().currentCall(),
        roundData.trucoStateMachine().caller(), roundData.trucoStateMachine().pointsAtStake());

    round.getEnvidoStateMachine()
        .initializeState(new ArrayList<>(roundData.envidoStateMachine().chain()),
            roundData.envidoStateMachine().resolved());

    return round;
  }

}
