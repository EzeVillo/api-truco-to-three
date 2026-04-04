package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LeagueQueryRepository {

  Optional<League> findById(LeagueId leagueId);

  Optional<League> findByInviteCode(InviteCode inviteCode);

  Optional<League> findByMatchId(MatchId matchId);

  Optional<League> findInProgressByPlayer(PlayerId playerId);

  Optional<League> findWaitingByPlayer(PlayerId playerId);

  List<LeagueId> findIdleLeagueIds(Instant idleSince);

  List<League> findPublicWaiting();

  CursorPageResult<League> findPublicWaiting(CursorPageQuery pageQuery);

}
