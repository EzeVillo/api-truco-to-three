package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.CreateTournamentCommand;
import com.villo.truco.application.commands.JoinTournamentCommand;
import com.villo.truco.application.commands.LeaveTournamentCommand;
import com.villo.truco.application.commands.RegisterTournamentMatchResultCommand;
import com.villo.truco.application.commands.StartTournamentCommand;
import com.villo.truco.application.ports.in.CreateTournamentUseCase;
import com.villo.truco.application.ports.in.GetTournamentStateUseCase;
import com.villo.truco.application.ports.in.JoinTournamentUseCase;
import com.villo.truco.application.ports.in.LeaveTournamentUseCase;
import com.villo.truco.application.ports.in.RegisterTournamentMatchResultUseCase;
import com.villo.truco.application.ports.in.StartTournamentUseCase;
import com.villo.truco.application.queries.GetTournamentStateQuery;
import com.villo.truco.infrastructure.http.dto.request.CreateTournamentRequest;
import com.villo.truco.infrastructure.http.dto.request.JoinTournamentRequest;
import com.villo.truco.infrastructure.http.dto.response.CreateTournamentResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.infrastructure.http.dto.response.JoinTournamentResponse;
import com.villo.truco.infrastructure.http.dto.response.TournamentStateResponse;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@Tag(name = "Torneos", description = "Endpoints para crear y consultar torneos")
public final class TournamentController {

  private static final Logger LOGGER = LoggerFactory.getLogger(TournamentController.class);

  private final CreateTournamentUseCase createTournament;
  private final JoinTournamentUseCase joinTournament;
  private final LeaveTournamentUseCase leaveTournament;
  private final StartTournamentUseCase startTournament;
  private final RegisterTournamentMatchResultUseCase registerTournamentMatchResult;
  private final GetTournamentStateUseCase getTournamentState;

  public TournamentController(final CreateTournamentUseCase createTournament,
      final JoinTournamentUseCase joinTournament, final LeaveTournamentUseCase leaveTournament,
      final StartTournamentUseCase startTournament,
      final RegisterTournamentMatchResultUseCase registerTournamentMatchResult,
      final GetTournamentStateUseCase getTournamentState) {

    this.createTournament = Objects.requireNonNull(createTournament);
    this.joinTournament = Objects.requireNonNull(joinTournament);
    this.leaveTournament = Objects.requireNonNull(leaveTournament);
    this.startTournament = Objects.requireNonNull(startTournament);
    this.registerTournamentMatchResult = Objects.requireNonNull(registerTournamentMatchResult);
    this.getTournamentState = Objects.requireNonNull(getTournamentState);
  }

  @PostMapping
  @Transactional
  @Operation(summary = "Crear torneo", description = "Crea un torneo round-robin. El creador se une automáticamente.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Torneo creado", content = @Content(schema = @Schema(implementation = CreateTournamentResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Datos inválidos para crear el torneo", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<CreateTournamentResponse> createTournament(
      @RequestBody final CreateTournamentRequest request, @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP createTournament requested: capacity={}, gamesToPlay={}", request.capacity(),
        request.gamesToPlay());

    final var dto = this.createTournament.handle(
        new CreateTournamentCommand(jwt.getSubject(), request.capacity(), request.gamesToPlay()));

    return ResponseEntity.ok(CreateTournamentResponse.from(dto));
  }

  @PostMapping("/join")
  @Transactional
  @Operation(summary = "Unirse a torneo", description = "Une el jugador autenticado al torneo usando el código de invitación", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Jugador unido correctamente", content = @Content(schema = @Schema(implementation = JoinTournamentResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Torneo no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Código inválido o estado inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<JoinTournamentResponse> joinTournament(
      @RequestBody final JoinTournamentRequest request, @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP joinTournament requested: inviteCode={}", request.inviteCode());

    final var dto = this.joinTournament.handle(
        new JoinTournamentCommand(jwt.getSubject(), request.inviteCode()));

    return ResponseEntity.ok(JoinTournamentResponse.from(dto));
  }

  @PostMapping("/{tournamentId}/leave")
  @Transactional
  @Operation(summary = "Salir de torneo", description = "Remueve al jugador del torneo (solo antes de que arranque)", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Jugador removido"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se puede salir en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> leaveTournament(
      @Parameter(description = "ID del torneo") @PathVariable final String tournamentId,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP leaveTournament requested: tournamentId={}, playerId={}", tournamentId,
        jwt.getSubject());

    this.leaveTournament.handle(new LeaveTournamentCommand(tournamentId, jwt.getSubject()));

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{tournamentId}/start")
  @Transactional
  @Operation(summary = "Iniciar torneo", description = "El creador inicia el torneo una vez que todos los jugadores se unieron", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Torneo iniciado"),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se puede iniciar en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> startTournament(
      @Parameter(description = "ID del torneo") @PathVariable final String tournamentId,
      @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP startTournament requested: tournamentId={}, playerId={}", tournamentId,
        jwt.getSubject());

    this.startTournament.handle(new StartTournamentCommand(tournamentId, jwt.getSubject()));

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{tournamentId}/matches/{matchId}/sync-result")
  @Transactional
  @Operation(summary = "Sincronizar resultado de partido", description = "Actualiza en el torneo el resultado final de una partida", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Resultado sincronizado"),
      @ApiResponse(responseCode = "404", description = "Torneo o partida no encontrados", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "No se puede sincronizar en el estado actual", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> syncMatchResult(
      @Parameter(description = "ID del torneo", example = "tournament-123") @PathVariable final String tournamentId,
      @Parameter(description = "ID de la partida", example = "match-123") @PathVariable final String matchId) {

    LOGGER.info("HTTP syncMatchResult requested: tournamentId={}, matchId={}", tournamentId,
        matchId);

    this.registerTournamentMatchResult.handle(
        new RegisterTournamentMatchResultCommand(tournamentId, matchId));

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{tournamentId}")
  @Operation(summary = "Obtener estado de torneo", description = "Devuelve estado completo, tabla y fixtures del torneo", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Estado del torneo", content = @Content(schema = @Schema(implementation = TournamentStateResponse.class))),
      @ApiResponse(responseCode = "404", description = "Torneo no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<TournamentStateResponse> getTournamentState(
      @Parameter(description = "ID del torneo", example = "tournament-123") @PathVariable final String tournamentId) {

    LOGGER.debug("HTTP getTournamentState requested: tournamentId={}", tournamentId);

    final var dto = this.getTournamentState.handle(new GetTournamentStateQuery(tournamentId));

    return ResponseEntity.ok(TournamentStateResponse.from(dto));
  }

}
