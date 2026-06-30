package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.assemblers.MatchStateDTOAssembler;
import com.villo.truco.application.dto.MatchStateDTO;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.queries.GetMatchStateQuery;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import java.util.Objects;

public final class GetMatchStateQueryHandler implements GetMatchStateUseCase {

  private final MatchQueryRepository queryRepository;
  private final SpectatorshipRepository spectatorshipRepository;
  private final MatchStateDTOAssembler dtoAssembler;

  public GetMatchStateQueryHandler(final MatchQueryRepository queryRepository,
      final SpectatorshipRepository spectatorshipRepository,
      final PublicActorResolver publicActorResolver, final long idleTimeoutMillis,
      final long lobbyTimeoutMillis) {

    this.queryRepository = Objects.requireNonNull(queryRepository);
    this.spectatorshipRepository = Objects.requireNonNull(spectatorshipRepository);
    this.dtoAssembler = new MatchStateDTOAssembler(publicActorResolver, idleTimeoutMillis,
        lobbyTimeoutMillis);
  }

  @Override
  public MatchStateDTO handle(final GetMatchStateQuery query) {

    final var match = this.queryRepository.findById(query.matchId())
        .orElseThrow(() -> new MatchNotFoundException(query.matchId()));

    match.validatePlayerInMatch(query.requestingPlayer());

    final var spectatorCount = this.spectatorshipRepository.countActiveByMatchId(match.getId());

    return this.dtoAssembler.toDto(match, query.requestingPlayer(), spectatorCount);
  }

}
