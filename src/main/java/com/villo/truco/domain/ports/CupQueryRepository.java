package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface CupQueryRepository {

  Optional<Cup> findById(CupId cupId);

  Optional<Cup> findByMatchId(MatchId matchId);

  Optional<Cup> findInProgressByPlayer(PlayerId playerId);

  Optional<Cup> findWaitingByPlayer(PlayerId playerId);

  Map<PlayerId, Cup> findInProgressByPlayers(Set<PlayerId> playerIds);

  Set<PlayerId> findPlayersWaitingInCup(Set<PlayerId> playerIds);

  List<CupId> findIdleCupIds(Instant idleSince);

  CursorPageResult<Cup> findPublicWaiting(CursorPageQuery pageQuery);

}
