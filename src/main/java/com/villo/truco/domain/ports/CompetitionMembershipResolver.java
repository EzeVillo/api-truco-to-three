package com.villo.truco.domain.ports;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public interface CompetitionMembershipResolver {

  boolean belongsToSameCompetition(MatchId matchId, PlayerId playerId);

}
