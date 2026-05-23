package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import java.time.Instant;

public record CupTimeoutEntry(CupId cupId, Instant lastActivityAt) {

}
