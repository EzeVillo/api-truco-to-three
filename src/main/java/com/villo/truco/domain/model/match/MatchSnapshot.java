package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.HandId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.match.valueobjects.RoundId;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public final class MatchSnapshot {

  private MatchSnapshot() {

  }

  public record MatchData(MatchId id, PlayerId playerOne, PlayerId playerTwo, InviteCode inviteCode,
                          MatchRules rules, MatchStatus status, int gamesWonPlayerOne,
                          int gamesWonPlayerTwo, int gameNumber, int scorePlayerOne,
                          int scorePlayerTwo, int roundNumber, boolean readyPlayerOne,
                          boolean readyPlayerTwo, PlayerId firstManoOfGame,
                          RoundData currentRound) {

  }

  public record RoundData(RoundId id, int roundNumber, PlayerId mano, PlayerId playerOne,
                          PlayerId playerTwo, HandData handPlayerOne, HandData handPlayerTwo,
                          List<PlayedHandData> playedHands, List<CardPlayData> currentHandCards,
                          TrucoData trucoStateMachine, EnvidoData envidoStateMachine,
                          RoundStatus status, PlayerId currentTurn, PlayerId turnBeforeTrucoCall,
                          PlayerId turnBeforeEnvidoCall) {

  }

  public record HandData(HandId id, List<Card> cards) {

  }

  public record PlayedHandData(Card cardMano, Card cardPie, PlayerId winner) {

  }

  public record CardPlayData(PlayerId playerId, Card card) {

  }

  public record TrucoData(TrucoCall currentCall, PlayerId caller, int pointsAtStake) {

  }

  public record EnvidoData(List<EnvidoCall> chain, boolean resolved) {

  }

}
