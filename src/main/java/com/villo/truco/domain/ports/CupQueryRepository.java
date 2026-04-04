package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CupQueryRepository {

  Optional<Cup> findById(CupId cupId);

  Optional<Cup> findByInviteCode(InviteCode inviteCode);

  Optional<Cup> findByMatchId(MatchId matchId);

  Optional<Cup> findInProgressByPlayer(PlayerId playerId);

  Optional<Cup> findWaitingByPlayer(PlayerId playerId);

  List<CupId> findIdleCupIds(Instant idleSince);

  CursorPageResult<Cup> findPublicWaiting(CursorPageQuery pageQuery);

}
