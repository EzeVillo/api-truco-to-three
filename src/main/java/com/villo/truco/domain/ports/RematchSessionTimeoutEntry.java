package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import java.time.Instant;

public record RematchSessionTimeoutEntry(RematchSessionId sessionId, Instant expiresAt) {

}
