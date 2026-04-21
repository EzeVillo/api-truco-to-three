package com.villo.truco.social.infrastructure.http;

import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.social.application.commands.AcceptResourceInvitationCommand;
import com.villo.truco.social.application.commands.CancelResourceInvitationCommand;
import com.villo.truco.social.application.commands.CreateResourceInvitationCommand;
import com.villo.truco.social.application.commands.DeclineResourceInvitationCommand;
import com.villo.truco.social.application.ports.in.AcceptResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.CancelResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.CreateResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.DeclineResourceInvitationUseCase;
import com.villo.truco.social.application.ports.in.GetResourceInvitationsUseCase;
import com.villo.truco.social.application.ports.in.GetSentResourceInvitationsUseCase;
import com.villo.truco.social.application.queries.GetResourceInvitationsQuery;
import com.villo.truco.social.application.queries.GetSentResourceInvitationsQuery;
import com.villo.truco.social.infrastructure.http.dto.request.CreateResourceInvitationRequest;
import com.villo.truco.social.infrastructure.http.dto.response.CreateResourceInvitationResponse;
import com.villo.truco.social.infrastructure.http.dto.response.IncomingResourceInvitationResponse;
import com.villo.truco.social.infrastructure.http.dto.response.OutgoingResourceInvitationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social")
@Tag(name = "Social", description = "Amistades, invitaciones y acceso rapido entre amigos")
public class ResourceInvitationController {

  private final CreateResourceInvitationUseCase createResourceInvitationUseCase;
  private final AcceptResourceInvitationUseCase acceptResourceInvitationUseCase;
  private final DeclineResourceInvitationUseCase declineResourceInvitationUseCase;
  private final CancelResourceInvitationUseCase cancelResourceInvitationUseCase;
  private final GetResourceInvitationsUseCase getResourceInvitationsUseCase;
  private final GetSentResourceInvitationsUseCase getSentResourceInvitationsUseCase;

  public ResourceInvitationController(
      final CreateResourceInvitationUseCase createResourceInvitationUseCase,
      final AcceptResourceInvitationUseCase acceptResourceInvitationUseCase,
      final DeclineResourceInvitationUseCase declineResourceInvitationUseCase,
      final CancelResourceInvitationUseCase cancelResourceInvitationUseCase,
      final GetResourceInvitationsUseCase getResourceInvitationsUseCase,
      final GetSentResourceInvitationsUseCase getSentResourceInvitationsUseCase) {

    this.createResourceInvitationUseCase = Objects.requireNonNull(createResourceInvitationUseCase);
    this.acceptResourceInvitationUseCase = Objects.requireNonNull(acceptResourceInvitationUseCase);
    this.declineResourceInvitationUseCase = Objects.requireNonNull(
        declineResourceInvitationUseCase);
    this.cancelResourceInvitationUseCase = Objects.requireNonNull(cancelResourceInvitationUseCase);
    this.getResourceInvitationsUseCase = Objects.requireNonNull(getResourceInvitationsUseCase);
    this.getSentResourceInvitationsUseCase = Objects.requireNonNull(
        getSentResourceInvitationsUseCase);
  }

  @PostMapping("/invitations")
  @Operation(summary = "Crear invitación a recurso", description = "Crea una invitación para que un amigo se una a una partida, liga o copa", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Invitación creada", content = @Content(schema = @Schema(implementation = CreateResourceInvitationResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body inválido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo crear la invitación", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<CreateResourceInvitationResponse> createInvitation(
      @Valid @RequestBody final CreateResourceInvitationRequest request,
      @AuthenticationPrincipal final Jwt jwt) {

    final var dto = this.createResourceInvitationUseCase.handle(
        new CreateResourceInvitationCommand(jwt.getSubject(), request.recipientUsername(),
            request.targetType(), request.targetId()));
    return ResponseEntity.ok(CreateResourceInvitationResponse.from(dto));
  }

  @PostMapping("/invitations/{id}/accept")
  @Operation(summary = "Aceptar invitación", description = "Acepta una invitación a un recurso y une al jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Invitación aceptada, jugador unido al recurso"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Invitación no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Invitación expirada o recurso no disponible", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> acceptInvitation(
      @Parameter(description = "ID de la invitación", example = "00000000-0000-0000-0000-000000000001") @PathVariable final String id,
      @AuthenticationPrincipal final Jwt jwt) {

    this.acceptResourceInvitationUseCase.handle(
        new AcceptResourceInvitationCommand(id, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/invitations/{id}/decline")
  @Operation(summary = "Rechazar invitación", description = "Rechaza una invitación a un recurso", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Invitación rechazada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Invitación no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo rechazar la invitación", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> declineInvitation(
      @Parameter(description = "ID de la invitación", example = "00000000-0000-0000-0000-000000000001") @PathVariable final String id,
      @AuthenticationPrincipal final Jwt jwt) {

    this.declineResourceInvitationUseCase.handle(
        new DeclineResourceInvitationCommand(id, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/invitations/incoming")
  @Operation(summary = "Obtener invitaciones recibidas", description = "Devuelve las invitaciones pendientes recibidas por el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lista de invitaciones", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IncomingResourceInvitationResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<List<IncomingResourceInvitationResponse>> getInvitations(
      @AuthenticationPrincipal final Jwt jwt) {

    final var items = this.getResourceInvitationsUseCase.handle(
            new GetResourceInvitationsQuery(jwt.getSubject())).stream()
        .map(IncomingResourceInvitationResponse::from).toList();
    return ResponseEntity.ok(items);
  }

  @GetMapping("/invitations/outgoing")
  @Operation(summary = "Obtener invitaciones enviadas", description = "Devuelve las invitaciones pendientes enviadas por el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lista de invitaciones enviadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OutgoingResourceInvitationResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<List<OutgoingResourceInvitationResponse>> getSentInvitations(
      @AuthenticationPrincipal final Jwt jwt) {

    final var items = this.getSentResourceInvitationsUseCase.handle(
            new GetSentResourceInvitationsQuery(jwt.getSubject())).stream()
        .map(OutgoingResourceInvitationResponse::from).toList();
    return ResponseEntity.ok(items);
  }

  @PostMapping("/invitations/{id}/cancel")
  @Operation(summary = "Cancelar invitación enviada", description = "Cancela una invitación pendiente enviada por el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Invitación cancelada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Invitación no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo cancelar la invitación", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> cancelInvitation(
      @Parameter(description = "ID de la invitación", example = "00000000-0000-0000-0000-000000000001") @PathVariable final String id,
      @AuthenticationPrincipal final Jwt jwt) {

    this.cancelResourceInvitationUseCase.handle(
        new CancelResourceInvitationCommand(id, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

}
