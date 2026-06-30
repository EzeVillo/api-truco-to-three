package com.villo.truco.social.infrastructure.http;

import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.social.application.commands.UpdateSocialPreferencesCommand;
import com.villo.truco.social.application.ports.in.GetSocialPreferencesUseCase;
import com.villo.truco.social.application.ports.in.UpdateSocialPreferencesUseCase;
import com.villo.truco.social.application.queries.GetSocialPreferencesQuery;
import com.villo.truco.social.infrastructure.http.dto.request.UpdateSocialPreferencesRequest;
import com.villo.truco.social.infrastructure.http.dto.response.SocialPreferencesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social/preferences")
@Tag(name = "Social", description = "Amistades, invitaciones y acceso rapido entre amigos")
public class SocialPreferencesController {

  private final GetSocialPreferencesUseCase getSocialPreferencesUseCase;
  private final UpdateSocialPreferencesUseCase updateSocialPreferencesUseCase;

  public SocialPreferencesController(final GetSocialPreferencesUseCase getSocialPreferencesUseCase,
      final UpdateSocialPreferencesUseCase updateSocialPreferencesUseCase) {

    this.getSocialPreferencesUseCase = Objects.requireNonNull(getSocialPreferencesUseCase);
    this.updateSocialPreferencesUseCase = Objects.requireNonNull(updateSocialPreferencesUseCase);
  }

  @GetMapping
  @Operation(summary = "Obtener mis preferencias sociales", description = "Devuelve las preferencias sociales del jugador autenticado, como si acepta recibir solicitudes de amistad", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Preferencias sociales", content = @Content(schema = @Schema(implementation = SocialPreferencesResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<SocialPreferencesResponse> getMyPreferences(
      @AuthenticationPrincipal final Jwt jwt) {

    final var dto = this.getSocialPreferencesUseCase.handle(
        new GetSocialPreferencesQuery(jwt.getSubject()));
    return ResponseEntity.ok(SocialPreferencesResponse.from(dto));
  }

  @PutMapping
  @Operation(summary = "Actualizar mis preferencias sociales", description = "Actualiza las preferencias sociales del jugador autenticado. Si se desactiva la recepción de solicitudes de amistad, los demás jugadores no podrán enviarle nuevas solicitudes (las pendientes previas no se ven afectadas)", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Preferencias actualizadas", content = @Content(schema = @Schema(implementation = SocialPreferencesResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body inválido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<SocialPreferencesResponse> updateMyPreferences(
      @Valid @RequestBody final UpdateSocialPreferencesRequest request,
      @AuthenticationPrincipal final Jwt jwt) {

    final var dto = this.updateSocialPreferencesUseCase.handle(
        new UpdateSocialPreferencesCommand(jwt.getSubject(), request.acceptsFriendRequests()));
    return ResponseEntity.ok(SocialPreferencesResponse.from(dto));
  }

}
