package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.AbandonMatchCommand;
import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.application.commands.CreateMatchCommand;
import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.commands.JoinMatchCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.application.commands.StartMatchCommand;
import com.villo.truco.application.ports.in.AbandonMatchUseCase;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.application.queries.GetMatchStateQuery;
import com.villo.truco.infrastructure.http.dto.request.CallEnvidoRequest;
import com.villo.truco.infrastructure.http.dto.request.CreateMatchRequest;
import com.villo.truco.infrastructure.http.dto.request.JoinMatchRequest;
import com.villo.truco.infrastructure.http.dto.request.PlayCardRequest;
import com.villo.truco.infrastructure.http.dto.request.RespondEnvidoRequest;
import com.villo.truco.infrastructure.http.dto.request.RespondTrucoRequest;
import com.villo.truco.infrastructure.http.dto.response.CreateMatchResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.infrastructure.http.dto.response.JoinMatchResponse;
import com.villo.truco.infrastructure.http.dto.response.MatchStateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/matches")
@Tag(name = "Partidas", description = "Endpoints para crear, unir y jugar partidas de Truco")
public class MatchController {

  private static final Logger LOGGER = LoggerFactory.getLogger(MatchController.class);

  private final CreateMatchUseCase createMatch;
  private final JoinMatchUseCase joinMatch;
  private final StartMatchUseCase startMatch;
  private final PlayCardUseCase playCard;
  private final CallTrucoUseCase callTruco;
  private final RespondTrucoUseCase respondTruco;
  private final CallEnvidoUseCase callEnvido;
  private final RespondEnvidoUseCase respondEnvido;
  private final FoldUseCase fold;
  private final AbandonMatchUseCase abandonMatch;
  private final GetMatchStateUseCase getMatchState;

  public MatchController(final CreateMatchUseCase createMatch, final JoinMatchUseCase joinMatch,
      final StartMatchUseCase startMatch, final PlayCardUseCase playCard,
      final CallTrucoUseCase callTruco, final RespondTrucoUseCase respondTruco,
      final CallEnvidoUseCase callEnvido, final RespondEnvidoUseCase respondEnvido,
      final FoldUseCase fold, final AbandonMatchUseCase abandonMatch,
      final GetMatchStateUseCase getMatchState) {

    this.createMatch = Objects.requireNonNull(createMatch);
    this.joinMatch = Objects.requireNonNull(joinMatch);
    this.startMatch = Objects.requireNonNull(startMatch);
    this.playCard = Objects.requireNonNull(playCard);
    this.callTruco = Objects.requireNonNull(callTruco);
    this.respondTruco = Objects.requireNonNull(respondTruco);
    this.callEnvido = Objects.requireNonNull(callEnvido);
    this.respondEnvido = Objects.requireNonNull(respondEnvido);
    this.fold = Objects.requireNonNull(fold);
    this.abandonMatch = Objects.requireNonNull(abandonMatch);
    this.getMatchState = Objects.requireNonNull(getMatchState);
  }

  @PostMapping
  @Operation(summary = "Crear partida", description = "Crea una nueva partida para el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Partida creada", content = @Content(schema = @Schema(implementation = CreateMatchResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se pudo crear la partida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<CreateMatchResponse> createMatch(
      @RequestBody final CreateMatchRequest request, @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP createMatch requested");
    final var dto = this.createMatch.handle(
        new CreateMatchCommand(jwt.getSubject(), request.gamesToPlay()));
    return ResponseEntity.ok(CreateMatchResponse.from(dto));
  }

  @PostMapping("/join")
  @Operation(summary = "Unirse a partida", description = "Une el jugador autenticado a una partida usando invite code", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Jugador unido correctamente", content = @Content(schema = @Schema(implementation = JoinMatchResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Partida no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Invite code inválido o estado inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<JoinMatchResponse> joinMatch(@RequestBody final JoinMatchRequest request,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP joinMatch requested: inviteCode={}", request.inviteCode());
    final var dto = this.joinMatch.handle(
        new JoinMatchCommand(jwt.getSubject(), request.inviteCode()));
    return ResponseEntity.ok(JoinMatchResponse.from(dto));
  }

  @PostMapping("/{matchId}/start")
  @Operation(summary = "Iniciar partida", description = "Inicia una partida. Requiere token Bearer del jugador de esa partida", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Partida iniciada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se puede iniciar en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> startMatch(
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    this.startMatch.handle(new StartMatchCommand(matchId, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{matchId}")
  @Operation(summary = "Obtener estado de partida", description = "Devuelve estado completo de la partida para el jugador autenticado", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Estado de partida", content = @Content(schema = @Schema(implementation = MatchStateResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Partida no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<MatchStateResponse> getMatchState(
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    final var state = this.getMatchState.handle(new GetMatchStateQuery(matchId, jwt.getSubject()));
    return ResponseEntity.ok(MatchStateResponse.from(state));
  }

  @PostMapping("/{matchId}/play-card")
  @Operation(summary = "Jugar carta", description = "Juega una carta de la mano actual", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Carta jugada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Jugada inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> playCard(
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId,
      @RequestBody final PlayCardRequest request, @AuthenticationPrincipal final Jwt jwt) {

    this.playCard.handle(
        new PlayCardCommand(matchId, jwt.getSubject(), request.suit(), request.number()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/truco")
  @Operation(summary = "Cantar Truco", description = "Realiza un canto de Truco", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Canto registrado"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Canto inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> callTruco(
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    this.callTruco.handle(new CallTrucoCommand(matchId, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/truco/respond")
  @Operation(summary = "Responder Truco", description = "Responde al último canto de Truco", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Respuesta registrada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Respuesta inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> respondTruco(
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId,
      @RequestBody final RespondTrucoRequest request, @AuthenticationPrincipal final Jwt jwt) {

    this.respondTruco.handle(
        new RespondTrucoCommand(matchId, jwt.getSubject(), request.response()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/envido")
  @Operation(summary = "Cantar Envido", description = "Realiza un canto de Envido", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Canto registrado"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Canto inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> callEnvido(
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId,
      @RequestBody final CallEnvidoRequest request, @AuthenticationPrincipal final Jwt jwt) {

    this.callEnvido.handle(new CallEnvidoCommand(matchId, jwt.getSubject(), request.call()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/envido/respond")
  @Operation(summary = "Responder Envido", description = "Responde al último canto de Envido", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Respuesta registrada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Respuesta inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> respondEnvido(
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId,
      @RequestBody final RespondEnvidoRequest request, @AuthenticationPrincipal final Jwt jwt) {

    this.respondEnvido.handle(
        new RespondEnvidoCommand(matchId, jwt.getSubject(), request.response()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/fold")
  @Operation(summary = "Irse al mazo", description = "Abandona la ronda actual", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Acción registrada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Acción inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> fold(
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    this.fold.handle(new FoldCommand(matchId, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{matchId}/abandon")
  @Operation(summary = "Abandonar partida", description = "El jugador autenticado abandona voluntariamente la partida; el oponente gana", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Partida abandonada"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se puede abandonar en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> abandonMatch(
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId,
      @AuthenticationPrincipal final Jwt jwt) {

    this.abandonMatch.handle(new AbandonMatchCommand(matchId, jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

}
