package com.villo.truco.domain.ports;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.time.Instant;

public record MatchTimeoutEntry(MatchId matchId, Instant lastActivityAt) {

}
