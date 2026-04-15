package com.villo.truco.social.infrastructure.http;

import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.social.application.commands.AcceptFriendshipCommand;
import com.villo.truco.social.application.commands.CancelFriendshipCommand;
import com.villo.truco.social.application.commands.DeclineFriendshipCommand;
import com.villo.truco.social.application.commands.RemoveFriendshipCommand;
import com.villo.truco.social.application.commands.RequestFriendshipCommand;
import com.villo.truco.social.application.ports.in.AcceptFriendshipUseCase;
import com.villo.truco.social.application.ports.in.CancelFriendshipUseCase;
import com.villo.truco.social.application.ports.in.DeclineFriendshipUseCase;
import com.villo.truco.social.application.ports.in.GetFriendsUseCase;
import com.villo.truco.social.application.ports.in.GetFriendshipRequestsUseCase;
import com.villo.truco.social.application.ports.in.GetSentFriendshipRequestsUseCase;
import com.villo.truco.social.application.ports.in.RemoveFriendshipUseCase;
import com.villo.truco.social.application.ports.in.RequestFriendshipUseCase;
import com.villo.truco.social.application.queries.GetFriendsQuery;
import com.villo.truco.social.application.queries.GetFriendshipRequestsQuery;
import com.villo.truco.social.application.queries.GetSentFriendshipRequestsQuery;
import com.villo.truco.social.infrastructure.http.dto.request.RequestFriendshipRequest;
import com.villo.truco.social.infrastructure.http.dto.response.FriendSummaryResponse;
import com.villo.truco.social.infrastructure.http.dto.response.IncomingFriendshipRequestResponse;
import com.villo.truco.social.infrastructure.http.dto.response.OutgoingFriendshipRequestResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social")
@Tag(name = "Social", description = "Amistades, invitaciones y acceso rapido entre amigos")
public class FriendshipController {

  private final RequestFriendshipUseCase requestFriendshipUseCase;
  private final AcceptFriendshipUseCase acceptFriendshipUseCase;
  private final DeclineFriendshipUseCase declineFriendshipUseCase;
  private final CancelFriendshipUseCase cancelFriendshipUseCase;
  private final RemoveFriendshipUseCase removeFriendshipUseCase;
  private final GetFriendsUseCase getFriendsUseCase;
  private final GetFriendshipRequestsUseCase getFriendshipRequestsUseCase;
  private final GetSentFriendshipRequestsUseCase getSentFriendshipRequestsUseCase;

  public FriendshipController(final RequestFriendshipUseCase requestFriendshipUseCase,
      final AcceptFriendshipUseCase acceptFriendshipUseCase,
      final DeclineFriendshipUseCase declineFriendshipUseCase,
      final CancelFriendshipUseCase cancelFriendshipUseCase,
      final RemoveFriendshipUseCase removeFriendshipUseCase,
      final GetFriendsUseCase getFriendsUseCase,
      final GetFriendshipRequestsUseCase getFriendshipRequestsUseCase,
      final GetSentFriendshipRequestsUseCase getSentFriendshipRequestsUseCase) {

    this.requestFriendshipUseCase = Objects.requireNonNull(requestFriendshipUseCase);
    this.acceptFriendshipUseCase = Objects.requireNonNull(acceptFriendshipUseCase);
    this.declineFriendshipUseCase = Objects.requireNonNull(declineFriendshipUseCase);
    this.cancelFriendshipUseCase = Objects.requireNonNull(cancelFriendshipUseCase);
    this.removeFriendshipUseCase = Objects.requireNonNull(removeFriendshipUseCase);
    this.getFriendsUseCase = Objects.requireNonNull(getFriendsUseCase);
    this.getFriendshipRequestsUseCase = Objects.requireNonNull(getFriendshipRequestsUseCase);
    this.getSentFriendshipRequestsUseCase = Objects.requireNonNull(
        getSentFriendshipRequestsUseCase);
  }

  @PostMapping("/friendship-requests")
  @Operation(summary = "Enviar solicitud de amistad", description = "Envía una solicitud de amistad al usuario indicado por nombre de usuario", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Solicitud creada"),
      @ApiResponse(responseCode = "400", description = "Body inválido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo crear la solicitud", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> requestFriendship(
      @Valid @RequestBody final RequestFriendshipRequest request,
      @AuthenticationPrincipal final Jwt jwt) {

    this.requestFriendshipUseCase.handle(
        new RequestFriendshipCommand(jwt.getSubject(), request.username()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/friendship-requests/{username}/accept")
  @Operation(summary = "Aceptar solicitud de amistad", description = "Acepta una solicitud de amistad pendiente recibida por el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Solicitud aceptada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo aceptar la solicitud", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> acceptFriendship(
      @Parameter(description = "Nombre de usuario del solicitante", example = "juancho") @PathVariable final String username,
      @AuthenticationPrincipal final Jwt jwt) {

    this.acceptFriendshipUseCase.handle(new AcceptFriendshipCommand(username, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/friendship-requests/{username}/decline")
  @Operation(summary = "Rechazar solicitud de amistad", description = "Rechaza una solicitud de amistad pendiente recibida por el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Solicitud rechazada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo rechazar la solicitud", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> declineFriendship(
      @Parameter(description = "Nombre de usuario del solicitante", example = "juancho") @PathVariable final String username,
      @AuthenticationPrincipal final Jwt jwt) {

    this.declineFriendshipUseCase.handle(new DeclineFriendshipCommand(username, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/friendship-requests/{username}/cancel")
  @Operation(summary = "Cancelar solicitud de amistad", description = "Cancela una solicitud de amistad pendiente enviada por el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Solicitud cancelada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Solicitud no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo cancelar la solicitud", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> cancelFriendship(
      @Parameter(description = "Nombre de usuario del destinatario", example = "martina") @PathVariable final String username,
      @AuthenticationPrincipal final Jwt jwt) {

    this.cancelFriendshipUseCase.handle(new CancelFriendshipCommand(username, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/friendships/{username}")
  @Operation(summary = "Eliminar amigo", description = "Elimina una amistad aceptada. Cualquiera de los dos jugadores puede eliminar al otro", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Amistad eliminada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Amistad no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo eliminar la amistad", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> removeFriendship(
      @Parameter(description = "Nombre de usuario del amigo", example = "martina") @PathVariable final String username,
      @AuthenticationPrincipal final Jwt jwt) {

    this.removeFriendshipUseCase.handle(new RemoveFriendshipCommand(username, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/friendships")
  @Operation(summary = "Obtener amigos", description = "Devuelve la lista de amigos aceptados del jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lista de amigos", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FriendSummaryResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<List<FriendSummaryResponse>> getFriends(
      @AuthenticationPrincipal final Jwt jwt) {

    final var items = this.getFriendsUseCase.handle(new GetFriendsQuery(jwt.getSubject())).stream()
        .map(FriendSummaryResponse::from).toList();
    return ResponseEntity.ok(items);
  }

  @GetMapping("/friendship-requests/incoming")
  @Operation(summary = "Obtener solicitudes de amistad recibidas", description = "Devuelve las solicitudes de amistad pendientes recibidas por el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lista de solicitudes", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IncomingFriendshipRequestResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<List<IncomingFriendshipRequestResponse>> getFriendshipRequests(
      @AuthenticationPrincipal final Jwt jwt) {

    final var items = this.getFriendshipRequestsUseCase.handle(
            new GetFriendshipRequestsQuery(jwt.getSubject())).stream()
        .map(IncomingFriendshipRequestResponse::from).toList();
    return ResponseEntity.ok(items);
  }

  @GetMapping("/friendship-requests/outgoing")
  @Operation(summary = "Obtener solicitudes de amistad enviadas", description = "Devuelve las solicitudes de amistad pendientes enviadas por el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lista de solicitudes enviadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OutgoingFriendshipRequestResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<List<OutgoingFriendshipRequestResponse>> getSentFriendshipRequests(
      @AuthenticationPrincipal final Jwt jwt) {

    final var items = this.getSentFriendshipRequestsUseCase.handle(
            new GetSentFriendshipRequestsQuery(jwt.getSubject())).stream()
        .map(OutgoingFriendshipRequestResponse::from).toList();
    return ResponseEntity.ok(items);
  }

}
