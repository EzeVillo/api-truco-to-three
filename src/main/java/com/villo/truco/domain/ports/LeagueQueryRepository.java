package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;

public interface LeagueQueryRepository {

  Optional<League> findById(LeagueId leagueId);

  Optional<League> findByInviteCode(InviteCode inviteCode);

  Optional<League> findByMatchId(MatchId matchId);

  Optional<League> findInProgressByPlayer(PlayerId playerId);

  Optional<League> findWaitingByPlayer(PlayerId playerId);

}
