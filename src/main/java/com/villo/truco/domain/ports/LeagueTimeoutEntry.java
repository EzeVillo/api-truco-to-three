package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import java.time.Instant;

public record LeagueTimeoutEntry(LeagueId leagueId, Instant lastActivityAt) {

}
