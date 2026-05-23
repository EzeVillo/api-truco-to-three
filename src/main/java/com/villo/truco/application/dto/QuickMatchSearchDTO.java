package com.villo.truco.application.dto;

import java.time.Instant;
import java.util.UUID;

public record QuickMatchSearchDTO(QuickMatchStatus status, UUID matchId, Instant enqueuedAt) {

}
