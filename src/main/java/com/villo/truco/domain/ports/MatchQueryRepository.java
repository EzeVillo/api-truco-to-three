package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;

public interface MatchQueryRepository {

  Optional<Match> findById(MatchId matchId);

  Optional<Match> findByInviteCode(InviteCode inviteCode);

  boolean hasActiveMatch(PlayerId playerId);

}
