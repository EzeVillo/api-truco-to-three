package com.villo.truco.domain.model.match;

import java.util.List;

public final class MatchSnapshotExtractor {

  private MatchSnapshotExtractor() {

  }

  public static MatchSnapshot extract(final Match match) {

    final var currentRound = match.getCurrentRound();
    final RoundSnapshot roundSnapshot = currentRound != null ? extractRound(currentRound) : null;

    return new MatchSnapshot(match.getId(), match.getPlayerOne(), match.getPlayerTwo(),
        match.getInviteCode(), match.getRules(), match.getStatus(), match.getGamesWonPlayerOne(),
        match.getGamesWonPlayerTwo(), match.getGameNumber(), match.getScorePlayerOne(),
        match.getScorePlayerTwo(), match.getRoundNumber(), match.isReadyPlayerOne(),
        match.isReadyPlayerTwo(), match.getFirstManoOfGame(), roundSnapshot);
  }

  private static RoundSnapshot extractRound(final Round round) {

    final var playedHands = round.getPlayedHandsInternal().stream()
        .map(ph -> new PlayedHandSnapshot(ph.cardMano(), ph.cardPie(), ph.winner())).toList();

    final var currentHandCards = round.getCurrentHandCards().stream()
        .map(cp -> new CardPlaySnapshot(cp.playerId(), cp.card())).toList();

    final var truco = extractTruco(round.getTrucoStateMachine());
    final var envido = extractEnvido(round.getEnvidoStateMachine());

    return new RoundSnapshot(round.getId(), round.getRoundNumber(), round.getMano(),
        round.getPlayerOne(), round.getPlayerTwo(), extractHand(round.getHandPlayerOne()),
        extractHand(round.getHandPlayerTwo()), playedHands, currentHandCards, truco, envido,
        round.getStatus(), round.getCurrentTurn(), round.getTurnBeforeTrucoCall(),
        round.getTurnBeforeEnvidoCall());
  }

  private static HandSnapshot extractHand(final Hand hand) {

    return new HandSnapshot(hand.getId(), List.copyOf(hand.getCards()));
  }

  private static TrucoSnapshot extractTruco(final TrucoStateMachine truco) {

    return new TrucoSnapshot(truco.getCurrentCall(), truco.getCaller(), truco.getPointsAtStake());
  }

  private static EnvidoSnapshot extractEnvido(final EnvidoStateMachine envido) {

    return new EnvidoSnapshot(List.copyOf(envido.getChain()), envido.isResolved());
  }

}
