package com.villo.truco.auth.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud con refresh token")
public record RefreshTokenRequest(
    @NotBlank @Schema(description = "Refresh token opaco", example = "7H5Q3v2...") String refreshToken) {

}
