package com.villo.truco.auth.domain.model.auth;

import com.villo.truco.auth.domain.model.auth.valueobjects.RefreshTokenId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;

public record RefreshTokenSnapshot(RefreshTokenId id, PlayerId userId, String tokenHash,
                                   Instant expiresAt, Instant createdAt, Instant revokedAt,
                                   Instant rotatedAt, RefreshTokenId replacedByTokenId) {

}
