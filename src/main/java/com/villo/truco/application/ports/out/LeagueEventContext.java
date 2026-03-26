package com.villo.truco.application.ports.out;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public record LeagueEventContext(LeagueId leagueId, List<PlayerId> participants) {

}
