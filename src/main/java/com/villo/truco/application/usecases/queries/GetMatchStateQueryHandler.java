package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.assemblers.MatchStateDTOAssembler;
import com.villo.truco.application.dto.MatchStateDTO;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.queries.GetMatchStateQuery;
import com.villo.truco.domain.ports.MatchQueryRepository;
import java.util.Objects;

public final class GetMatchStateQueryHandler implements GetMatchStateUseCase {

  private final MatchQueryRepository queryRepository;
  private final MatchStateDTOAssembler dtoAssembler;

  public GetMatchStateQueryHandler(final MatchQueryRepository queryRepository,
      final PublicActorResolver publicActorResolver) {

    this.queryRepository = Objects.requireNonNull(queryRepository);
    this.dtoAssembler = new MatchStateDTOAssembler(publicActorResolver);
  }

  @Override
  public MatchStateDTO handle(final GetMatchStateQuery query) {

    final var match = this.queryRepository.findById(query.matchId())
        .orElseThrow(() -> new MatchNotFoundException(query.matchId()));

    match.validatePlayerInMatch(query.requestingPlayer());

    return this.dtoAssembler.toDto(match, query.requestingPlayer());
  }

}
