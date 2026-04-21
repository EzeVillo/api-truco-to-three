package com.villo.truco.profile.domain.model;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public record MatchAchievementTrackerSnapshot(MatchId matchId, PlayerId playerOne,
                                              PlayerId playerTwo, boolean humanVsHuman,
                                              int currentGameNumber, int currentRoundNumber,
                                              PlayerSeat manoSeat, int previousScorePlayerOne,
                                              int previousScorePlayerTwo, int scorePlayerOne,
                                              int scorePlayerTwo, int playedHandsInRound,
                                              boolean roundHadCalls,
                                              List<EnvidoCall> envidoCallsInRound,
                                              EnvidoResponse lastEnvidoResponse,
                                              PlayerSeat lastEnvidoWinnerSeat,
                                              Integer lastEnvidoPointsMano,
                                              Integer lastEnvidoPointsPie,
                                              TrucoResponse lastTrucoResponse,
                                              TrucoCall lastTrucoResponseCall,
                                              PlayerSeat lastTrucoResponderSeat,
                                              Card lastHandCardPlayerOne,
                                              Card lastHandCardPlayerTwo,
                                              PlayerSeat lastHandWinnerSeat,
                                              PlayerSeat lastFoldedSeat,
                                              PlayerSeat lastRoundWinnerSeat,
                                              boolean pendingAcceptedValeCuatro,
                                              boolean playerOneLostAcceptedValeCuatroByBust,
                                              boolean playerTwoLostAcceptedValeCuatroByBust) {

}
