package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface LeagueQueryRepository {

  Optional<League> findById(LeagueId leagueId);

  Optional<League> findByMatchId(MatchId matchId);

  Optional<League> findInProgressByPlayer(PlayerId playerId);

  Optional<League> findWaitingByPlayer(PlayerId playerId);

  Map<PlayerId, League> findInProgressByPlayers(Set<PlayerId> playerIds);

  Set<PlayerId> findPlayersWaitingInLeague(Set<PlayerId> playerIds);

  List<LeagueId> findIdleLeagueIds(Instant idleSince);

  List<League> findPublicWaiting();

  CursorPageResult<League> findPublicWaiting(CursorPageQuery pageQuery);

}
