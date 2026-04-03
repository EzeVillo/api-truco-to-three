package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.assemblers.SpectatorMatchStateDTOAssembler;
import com.villo.truco.application.dto.SpectatorMatchStateDTO;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.application.ports.in.GetSpectateMatchStateUseCase;
import com.villo.truco.application.queries.GetSpectateMatchStateQuery;
import com.villo.truco.domain.model.spectator.exceptions.NotSpectatingException;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import java.util.Objects;

public final class GetSpectateMatchStateQueryHandler implements GetSpectateMatchStateUseCase {

  private final MatchQueryRepository matchQueryRepository;
  private final SpectatorshipRepository spectatorshipRepository;
  private final SpectatorMatchStateDTOAssembler dtoAssembler;

  public GetSpectateMatchStateQueryHandler(final MatchQueryRepository matchQueryRepository,
      final SpectatorshipRepository spectatorshipRepository,
      final SpectatorMatchStateDTOAssembler dtoAssembler) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.spectatorshipRepository = Objects.requireNonNull(spectatorshipRepository);
    this.dtoAssembler = Objects.requireNonNull(dtoAssembler);
  }

  @Override
  public SpectatorMatchStateDTO handle(final GetSpectateMatchStateQuery query) {

    final var spectatedMatch = this.spectatorshipRepository.findBySpectatorId(query.spectatorId())
        .flatMap(spectatorship -> spectatorship.getActiveMatchId()
            .filter(matchId -> matchId.equals(query.matchId())))
        .orElseThrow(() -> new NotSpectatingException(query.spectatorId()));

    final var match = this.matchQueryRepository.findById(spectatedMatch)
        .orElseThrow(() -> new MatchNotFoundException(spectatedMatch));

    return this.dtoAssembler.toDto(match,
        this.spectatorshipRepository.countActiveByMatchId(spectatedMatch));
  }

}
