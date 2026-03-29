package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import java.util.List;
import java.util.Objects;

public record MatchPlayerDecisionView(GameContext game, TrucoContext truco, EnvidoContext envido) {

  public static MatchPlayerDecisionView empty(final int myScore, final int rivalScore) {

    return new MatchPlayerDecisionView(
        new GameContext(List.of(), myScore, rivalScore, null, 0, 0, false, false, false, false,
            ScoringPolicy.pointsToWinGame()), new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
  }

  public boolean hasAvailableActions() {

    return this.game.canPlayCard() || this.game.canFold() || this.truco.availableCall() != null
        || !this.truco.availableResponses().isEmpty() || !this.envido.availableCalls().isEmpty()
        || !this.envido.availableResponses().isEmpty();
  }

  public record GameContext(List<CardView> myCards, int myScore, int rivalScore,
                            CardView rivalCardPlayed, int envidoScore, int handsPlayedCount,
                            boolean isMano, boolean canPlayCard, boolean canFold,
                            boolean foldWouldGiveGameToBot, int pointsToWin) {

    public GameContext {

      myCards = List.copyOf(Objects.requireNonNull(myCards));
    }

  }

  public record TrucoContext(TrucoCall availableCall, List<TrucoResponse> availableResponses,
                             TrucoCall currentCall) {

    public TrucoContext {

      availableResponses = List.copyOf(Objects.requireNonNull(availableResponses));
    }

  }

  public record EnvidoContext(List<EnvidoOption> availableCalls,
                              List<EnvidoResponse> availableResponses,
                              List<EnvidoOption> currentChain,
                              PendingEnvidoOutcome pendingOutcome) {

    public EnvidoContext {

      availableCalls = List.copyOf(Objects.requireNonNull(availableCalls));
      availableResponses = List.copyOf(Objects.requireNonNull(availableResponses));
      currentChain = List.copyOf(Objects.requireNonNull(currentChain));
    }

  }

  public record CardView(int trucoValue, Card card) {

    public CardView {

      Objects.requireNonNull(card);
    }

  }

  public record EnvidoOption(EnvidoCall call, int pointsIfPlayerWins, int pointsIfRivalWins) {

    public EnvidoOption {

      Objects.requireNonNull(call);
    }

  }

  public record PendingEnvidoOutcome(int acceptedPointsIfPlayerWins, int acceptedPointsIfRivalWins,
                                     int rejectedPoints) {

  }

}
