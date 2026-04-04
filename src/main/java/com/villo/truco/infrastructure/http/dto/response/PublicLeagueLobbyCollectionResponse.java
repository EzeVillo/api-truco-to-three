package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.PublicLeagueLobbyDTO;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Coleccion paginada de lobbies publicos de ligas")
public record PublicLeagueLobbyCollectionResponse(
    @Schema(description = "Items de la pagina actual") List<PublicLeagueLobbyResponse> items,
    @Schema(description = "Links HATEOAS de la coleccion") CursorPageLinksResponse _links) {

  public static PublicLeagueLobbyCollectionResponse from(
      final CursorPageResult<PublicLeagueLobbyDTO> page, final int limit, final String after) {

    return new PublicLeagueLobbyCollectionResponse(
        page.items().stream().map(PublicLeagueLobbyResponse::from).toList(),
        PublicLobbyLinkFactory.collectionLinks("/api/leagues/public", limit, after,
            page.nextCursor()));
  }

}
