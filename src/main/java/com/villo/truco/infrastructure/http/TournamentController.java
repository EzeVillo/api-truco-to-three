package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.CreateTournamentCommand;
import com.villo.truco.application.commands.RegisterTournamentMatchResultCommand;
import com.villo.truco.application.ports.in.CreateTournamentUseCase;
import com.villo.truco.application.ports.in.GetTournamentStateUseCase;
import com.villo.truco.application.ports.in.RegisterTournamentMatchResultUseCase;
import com.villo.truco.application.queries.GetTournamentStateQuery;
import com.villo.truco.infrastructure.http.dto.request.CreateTournamentRequest;
import com.villo.truco.infrastructure.http.dto.response.CreateTournamentResponse;
import com.villo.truco.infrastructure.http.dto.response.TournamentStateResponse;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
public final class TournamentController {

  private final CreateTournamentUseCase createTournament;
  private final RegisterTournamentMatchResultUseCase registerTournamentMatchResult;
  private final GetTournamentStateUseCase getTournamentState;

  public TournamentController(final CreateTournamentUseCase createTournament,
      final RegisterTournamentMatchResultUseCase registerTournamentMatchResult,
      final GetTournamentStateUseCase getTournamentState) {

    this.createTournament = Objects.requireNonNull(createTournament);
    this.registerTournamentMatchResult = Objects.requireNonNull(registerTournamentMatchResult);
    this.getTournamentState = Objects.requireNonNull(getTournamentState);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<CreateTournamentResponse> createTournament(
      @RequestBody final CreateTournamentRequest request) {

    final var dto = this.createTournament.handle(
        CreateTournamentCommand.fromPlayerIds(request.playerIds()));

    return ResponseEntity.ok(CreateTournamentResponse.from(dto));
  }

  @PostMapping("/{tournamentId}/matches/{matchId}/sync-result")
  @Transactional
  public ResponseEntity<Void> syncMatchResult(@PathVariable final String tournamentId,
      @PathVariable final String matchId) {

    this.registerTournamentMatchResult.handle(
        new RegisterTournamentMatchResultCommand(tournamentId, matchId));

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{tournamentId}")
  public ResponseEntity<TournamentStateResponse> getTournamentState(
      @PathVariable final String tournamentId) {

    final var dto = this.getTournamentState.handle(new GetTournamentStateQuery(tournamentId));

    return ResponseEntity.ok(TournamentStateResponse.from(dto));
  }

}
