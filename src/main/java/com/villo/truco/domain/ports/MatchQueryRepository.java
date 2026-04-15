package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MatchQueryRepository {

  Optional<Match> findById(MatchId matchId);

  boolean hasActiveMatch(PlayerId playerId);

  boolean hasUnfinishedMatch(PlayerId playerId);

  List<MatchId> findIdleMatchIds(Instant idleSince);

  List<Match> findPublicWaiting();

  CursorPageResult<Match> findPublicWaiting(CursorPageQuery pageQuery);

}
