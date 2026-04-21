package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.JoinByCodeCommand;
import com.villo.truco.application.ports.in.JoinByCodeUseCase;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.infrastructure.http.dto.response.JoinResourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/join")
@Tag(name = "Join", description = "Endpoint global para unirse a recursos compartibles")
public class JoinController {

  private final JoinByCodeUseCase joinByCodeUseCase;

  public JoinController(final JoinByCodeUseCase joinByCodeUseCase) {

    this.joinByCodeUseCase = Objects.requireNonNull(joinByCodeUseCase);
  }

  @PostMapping("/{joinCode}")
  @Operation(summary = "Unirse por joinCode", description = "Resuelve el joinCode global y une al usuario autenticado al recurso correspondiente", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Join resuelto correctamente", content = @Content(schema = @Schema(implementation = JoinResourceResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "joinCode no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "Otro request consumio el ultimo cupo del lobby publico", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "El recurso no admite join en su estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<JoinResourceResponse> join(
      @Parameter(description = "Join code compartible del recurso", example = "ABCD1234") @PathVariable final String joinCode,
      @AuthenticationPrincipal final Jwt jwt) {

    final var dto = this.joinByCodeUseCase.handle(
        new JoinByCodeCommand(jwt.getSubject(), joinCode));
    return ResponseEntity.ok(JoinResourceResponse.from(dto));
  }

}
