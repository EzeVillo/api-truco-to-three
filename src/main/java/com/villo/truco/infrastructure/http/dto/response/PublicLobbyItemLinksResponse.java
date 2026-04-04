package com.villo.truco.infrastructure.http.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Links accionables del item")
public record PublicLobbyItemLinksResponse(
    @Schema(description = "Endpoint para unirse al lobby publico") LinkResponse joinPublic) {

}
