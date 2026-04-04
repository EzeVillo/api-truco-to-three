package com.villo.truco.domain.shared.pagination;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public record PublicLobbyCursor(Instant lastActivityAt, UUID resourceId) {

  private static final String SEPARATOR = "|";

  public PublicLobbyCursor {

    Objects.requireNonNull(lastActivityAt);
    Objects.requireNonNull(resourceId);
  }

  public static PublicLobbyCursor decode(final String encodedCursor) {

    try {
      final var decoded = new String(Base64.getUrlDecoder().decode(encodedCursor),
          StandardCharsets.UTF_8);
      final var parts = decoded.split("\\|", -1);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Cursor must contain exactly two parts");
      }

      return new PublicLobbyCursor(Instant.ofEpochMilli(Long.parseLong(parts[0])),
          UUID.fromString(parts[1]));
    } catch (final RuntimeException ex) {
      throw new IllegalArgumentException("Invalid public lobby cursor", ex);
    }
  }

  public String encode() {

    final var rawValue = this.lastActivityAt.toEpochMilli() + SEPARATOR + this.resourceId;
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(rawValue.getBytes(StandardCharsets.UTF_8));
  }

}
