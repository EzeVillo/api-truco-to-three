package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.PublicMatchLobbyDTO;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Coleccion paginada de lobbies publicos de partidas")
public record PublicMatchLobbyCollectionResponse(
    @Schema(description = "Items de la pagina actual") List<PublicMatchLobbyResponse> items,
    @Schema(description = "Links HATEOAS de la coleccion") CursorPageLinksResponse _links) {

  public static PublicMatchLobbyCollectionResponse from(
      final CursorPageResult<PublicMatchLobbyDTO> page, final int limit, final String after) {

    return new PublicMatchLobbyCollectionResponse(
        page.items().stream().map(PublicMatchLobbyResponse::from).toList(),
        PublicLobbyLinkFactory.collectionLinks("/api/matches/public", limit, after,
            page.nextCursor()));
  }

}
