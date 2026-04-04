package com.villo.truco.infrastructure.http.dto.response;

import java.util.Objects;

final class PublicLobbyLinkFactory {

  private PublicLobbyLinkFactory() {

  }

  static CursorPageLinksResponse collectionLinks(final String collectionPath, final int limit,
      final String after, final String nextCursor) {

    return new CursorPageLinksResponse(
        new LinkResponse(collectionHref(collectionPath, limit, after)), nextCursor == null ? null
        : new LinkResponse(collectionHref(collectionPath, limit, nextCursor)));
  }

  static PublicLobbyItemLinksResponse itemLinks(final String resourcePath,
      final String resourceId) {

    return new PublicLobbyItemLinksResponse(
        new LinkResponse(Objects.requireNonNull(resourcePath) + "/" + resourceId + "/join-public"));
  }

  private static String collectionHref(final String collectionPath, final int limit,
      final String after) {

    final var builder = new StringBuilder(Objects.requireNonNull(collectionPath)).append("?limit=")
        .append(limit);
    if (after != null && !after.isBlank()) {
      builder.append("&after=").append(after);
    }
    return builder.toString();
  }

}
