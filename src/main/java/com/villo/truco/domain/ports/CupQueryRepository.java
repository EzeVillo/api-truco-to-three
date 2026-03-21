package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import java.util.Optional;

public interface CupQueryRepository {

  Optional<Cup> findById(CupId cupId);

  Optional<Cup> findByInviteCode(InviteCode inviteCode);

  Optional<Cup> findByMatchId(MatchId matchId);

}
