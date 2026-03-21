package com.villo.truco.domain.model.cup;

import com.villo.truco.domain.model.cup.valueobjects.BoutId;
import com.villo.truco.domain.model.cup.valueobjects.BoutStatus;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Set;

public final class CupSnapshot {

  private CupSnapshot() {

  }

  public record CupData(CupId id, List<PlayerId> participants, List<BoutData> bouts,
                        Set<PlayerId> forfeitedPlayers, int numberOfPlayers,
                        GamesToPlay gamesToPlay, InviteCode inviteCode, CupStatus status,
                        PlayerId champion) {

  }

  public record BoutData(BoutId id, int roundNumber, int bracketPosition, PlayerId playerOne,
                         PlayerId playerTwo, MatchId matchId, PlayerId winner, BoutStatus status) {

  }

}
