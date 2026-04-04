package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.PublicCupLobbyDTO;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Coleccion paginada de lobbies publicos de copas")
public record PublicCupLobbyCollectionResponse(
    @Schema(description = "Items de la pagina actual") List<PublicCupLobbyResponse> items,
    @Schema(description = "Links HATEOAS de la coleccion") CursorPageLinksResponse _links) {

  public static PublicCupLobbyCollectionResponse from(
      final CursorPageResult<PublicCupLobbyDTO> page, final int limit, final String after) {

    return new PublicCupLobbyCollectionResponse(
        page.items().stream().map(PublicCupLobbyResponse::from).toList(),
        PublicLobbyLinkFactory.collectionLinks("/api/cups/public", limit, after,
            page.nextCursor()));
  }

}
