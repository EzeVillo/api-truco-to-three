package com.villo.truco.infrastructure.http.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Link HATEOAS minimo")
public record LinkResponse(
    @Schema(description = "URL relativa del recurso", example = "/api/matches/public?limit=20") String href) {

}
