package com.villo.truco.campaign.infrastructure.http;

import com.villo.truco.campaign.application.usecases.commands.StartCampaignChallengeCommand;
import com.villo.truco.campaign.application.usecases.commands.StartCampaignChallengeUseCase;
import com.villo.truco.campaign.application.usecases.queries.GetCampaignQuery;
import com.villo.truco.campaign.application.usecases.queries.GetCampaignUseCase;
import com.villo.truco.campaign.infrastructure.http.dto.request.StartCampaignChallengeRequest;
import com.villo.truco.campaign.infrastructure.http.dto.response.CampaignResponse;
import com.villo.truco.campaign.infrastructure.http.dto.response.StartCampaignChallengeResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaign")
@Tag(name = "Campaña", description = "Modo campaña: ranking de 100 bots, desafíos al mejor de 5 y progreso del jugador")
public class CampaignController {

  private final GetCampaignUseCase getCampaign;
  private final StartCampaignChallengeUseCase startCampaignChallenge;

  public CampaignController(final GetCampaignUseCase getCampaign,
      final StartCampaignChallengeUseCase startCampaignChallenge) {

    this.getCampaign = Objects.requireNonNull(getCampaign);
    this.startCampaignChallenge = Objects.requireNonNull(startCampaignChallenge);
  }

  @GetMapping
  @Operation(summary = "Consultar la campaña", description = "Devuelve el ranking completo (100 bots + jugador), el progreso del jugador y qué rival puede desafiar", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Estado de la campaña", content = @Content(schema = @Schema(implementation = CampaignResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<CampaignResponse> getCampaign(@AuthenticationPrincipal final Jwt jwt) {

    final var dto = this.getCampaign.handle(new GetCampaignQuery(jwt.getSubject()));
    return ResponseEntity.ok(CampaignResponse.from(dto));
  }

  @PostMapping("/challenges")
  @Operation(summary = "Desafiar a un rival de la campaña", description = "Crea una partida al mejor de 5 contra el bot inmediatamente superior en el ranking. El body es opcional: botId solo se acepta cuando el jugador ya alcanzó el puesto 1, lo que desbloquea desafiar a cualquier rival. Las partidas de campaña no ofrecen revancha.", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Desafío creado", content = @Content(schema = @Schema(implementation = StartCampaignChallengeResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body inválido o botId requerido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Bot no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Desafío no permitido (rival inválido o desafío ya activo)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<StartCampaignChallengeResponse> startChallenge(
      @RequestBody(required = false) final StartCampaignChallengeRequest request,
      @AuthenticationPrincipal final Jwt jwt) {

    final var botId = request == null ? null : request.botId();
    final var dto = this.startCampaignChallenge.handle(
        new StartCampaignChallengeCommand(jwt.getSubject(), botId));
    return ResponseEntity.ok(StartCampaignChallengeResponse.from(dto));
  }

}
