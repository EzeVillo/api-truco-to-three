package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import java.time.Instant;

public record LeagueTimeoutEntry(LeagueId leagueId, Instant lastActivityAt, LeagueStatus status) {

}
