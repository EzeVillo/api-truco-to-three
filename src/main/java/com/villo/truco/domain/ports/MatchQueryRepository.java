package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MatchQueryRepository {

  Optional<Match> findById(MatchId matchId);

  boolean hasActiveMatch(PlayerId playerId);

  boolean hasUnfinishedMatch(PlayerId playerId);

  Optional<Match> findUnfinishedByPlayer(PlayerId playerId);

  Set<PlayerId> findPlayersWithUnfinishedMatch(Set<PlayerId> playerIds);

  Map<PlayerId, Match> findUnfinishedByPlayers(Set<PlayerId> playerIds);

  List<MatchId> findIdleMatchIds(Instant idleSince);

  List<Match> findPublicWaiting();

  CursorPageResult<Match> findPublicWaiting(CursorPageQuery pageQuery);

}
