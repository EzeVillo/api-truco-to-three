package com.villo.truco.infrastructure.http.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Links del recurso de coleccion")
public record CursorPageLinksResponse(
    @Schema(description = "Link a la pagina actual") LinkResponse self,
    @Schema(description = "Link a la siguiente pagina si existe") LinkResponse next) {

}
