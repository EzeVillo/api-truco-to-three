package com.villo.truco.auth.infrastructure.http.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de registro de usuario")
public record RegisterUserRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9]+$", message = "must contain only letters and numbers") @Pattern(regexp = "^(?=(?:.*[A-Za-z]){3,}).+$", message = "must contain at least 3 letters") @Schema(description = "Nombre de usuario unico", example = "juancho") String username,
    @NotBlank @Size(min = 5, message = "must have at least 5 characters") @Pattern(regexp = ".*\\d.*", message = "must contain at least 1 number") @Pattern(regexp = ".*[^A-Za-z0-9].*", message = "must contain at least 1 symbol") @Schema(description = "Contrasena del usuario", example = "Clave1!") String password) {

}
