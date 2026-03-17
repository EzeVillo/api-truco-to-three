package com.villo.truco.domain.model.match;

import java.util.List;

public final class MatchSnapshotExtractor {

  private MatchSnapshotExtractor() {

  }

  public static MatchSnapshot.MatchData extract(final Match match) {

    final var currentRound = match.getCurrentRound();
    final MatchSnapshot.RoundData roundData =
        currentRound != null ? extractRound(currentRound) : null;

    return new MatchSnapshot.MatchData(match.getId(), match.getPlayerOne(), match.getPlayerTwo(),
        match.getInviteCode(), match.getRules(), match.getStatus(), match.getGamesWonPlayerOne(),
        match.getGamesWonPlayerTwo(), match.getGameNumber(), match.getScorePlayerOne(),
        match.getScorePlayerTwo(), match.getRoundNumber(), match.isReadyPlayerOne(),
        match.isReadyPlayerTwo(), match.getFirstManoOfGame(), roundData);
  }

  private static MatchSnapshot.RoundData extractRound(final Round round) {

    final var playedHands = round.getPlayedHandsInternal().stream()
        .map(ph -> new MatchSnapshot.PlayedHandData(ph.cardMano(), ph.cardPie(), ph.winner()))
        .toList();

    final var currentHandCards = round.getCurrentHandCards().stream()
        .map(cp -> new MatchSnapshot.CardPlayData(cp.playerId(), cp.card())).toList();

    final var truco = extractTruco(round.getTrucoStateMachine());
    final var envido = extractEnvido(round.getEnvidoStateMachine());

    return new MatchSnapshot.RoundData(round.getId(), round.getRoundNumber(), round.getMano(),
        round.getPlayerOne(), round.getPlayerTwo(), extractHand(round.getHandPlayerOne()),
        extractHand(round.getHandPlayerTwo()), playedHands, currentHandCards, truco, envido,
        round.getStatus(), round.getCurrentTurn(), round.getTurnBeforeTrucoCall(),
        round.getTurnBeforeEnvidoCall());
  }

  private static MatchSnapshot.HandData extractHand(final Hand hand) {

    return new MatchSnapshot.HandData(hand.getId(), List.copyOf(hand.getCards()));
  }

  private static MatchSnapshot.TrucoData extractTruco(final TrucoStateMachine truco) {

    return new MatchSnapshot.TrucoData(truco.getCurrentCall(), truco.getCaller(),
        truco.getPointsAtStake());
  }

  private static MatchSnapshot.EnvidoData extractEnvido(final EnvidoStateMachine envido) {

    return new MatchSnapshot.EnvidoData(List.copyOf(envido.getChain()), envido.isResolved());
  }

}
