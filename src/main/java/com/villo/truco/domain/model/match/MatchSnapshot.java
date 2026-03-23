package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record MatchSnapshot(MatchId id, PlayerId playerOne, PlayerId playerTwo,
                            InviteCode inviteCode, MatchRules rules, MatchStatus status,
                            int gamesWonPlayerOne, int gamesWonPlayerTwo, int gameNumber,
                            int scorePlayerOne, int scorePlayerTwo, int roundNumber,
                            boolean readyPlayerOne, boolean readyPlayerTwo,
                            PlayerId firstManoOfGame, RoundSnapshot currentRound) {

}
