package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import java.time.Instant;

public record CupTimeoutEntry(CupId cupId, Instant lastActivityAt, CupStatus status) {

}
