package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.MatchStateDTO;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.queries.GetMatchStateQuery;
import com.villo.truco.domain.ports.MatchQueryRepository;
import java.util.Objects;

public final class GetMatchStateQueryHandler implements GetMatchStateUseCase {

  private final MatchQueryRepository queryRepository;

  public GetMatchStateQueryHandler(final MatchQueryRepository queryRepository) {

    this.queryRepository = Objects.requireNonNull(queryRepository);
  }

  @Override
  public MatchStateDTO handle(final GetMatchStateQuery query) {

    final var match = this.queryRepository.findById(query.matchId())
        .orElseThrow(() -> new MatchNotFoundException(query.matchId()));

    return MatchStateDTO.of(match, query.requestingPlayer());
  }

}
