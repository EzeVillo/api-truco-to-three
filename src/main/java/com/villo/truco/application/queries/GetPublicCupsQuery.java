package com.villo.truco.application.queries;

import com.villo.truco.application.exceptions.InvalidCursorPageRequestException;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.PublicLobbyCursor;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record GetPublicCupsQuery(PlayerId playerId, CursorPageQuery pageQuery) {

  public static final int MAX_LIMIT = 100;

  public GetPublicCupsQuery(final String playerId, final int limit, final String after) {

    this(PlayerId.of(playerId), pageQuery(limit, after));
  }

  private static CursorPageQuery pageQuery(final int limit, final String after) {

    validateLimit(limit);
    validateCursor(after);
    return new CursorPageQuery(limit, normalizeCursor(after));
  }

  private static void validateLimit(final int limit) {

    if (limit < 1 || limit > MAX_LIMIT) {
      throw new InvalidCursorPageRequestException("limit must be between 1 and " + MAX_LIMIT);
    }
  }

  private static void validateCursor(final String after) {

    if (after == null || after.isBlank()) {
      return;
    }

    try {
      PublicLobbyCursor.decode(after);
    } catch (final IllegalArgumentException ex) {
      throw new InvalidCursorPageRequestException("after cursor is invalid");
    }
  }

  private static String normalizeCursor(final String after) {

    return after == null || after.isBlank() ? null : after;
  }

}
