package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.RematchSessionStateDTO;
import com.villo.truco.application.exceptions.RematchSessionNotFoundException;
import com.villo.truco.application.ports.in.GetRematchSessionUseCase;
import com.villo.truco.application.queries.GetRematchSessionQuery;
import com.villo.truco.domain.model.rematch.exceptions.NotParticipantOfRematchSessionException;
import com.villo.truco.domain.ports.RematchSessionRepository;
import java.util.Objects;

public final class GetRematchSessionQueryHandler implements GetRematchSessionUseCase {

  private final RematchSessionRepository repository;

  public GetRematchSessionQueryHandler(final RematchSessionRepository repository) {

    this.repository = Objects.requireNonNull(repository);
  }

  @Override
  public RematchSessionStateDTO handle(final GetRematchSessionQuery query) {

    final var session = repository.findByOriginMatchId(query.originMatchId())
        .orElseThrow(RematchSessionNotFoundException::new);

    if (!session.isParticipant(query.requester())) {
      throw new NotParticipantOfRematchSessionException();
    }

    return new RematchSessionStateDTO(session.getId().value().toString(),
        session.getOriginMatchId().value().toString(), session.getPlayerOneId().value().toString(),
        session.getPlayerTwoId().value().toString(), session.getStatus(),
        session.getPlayerOneChoice(), session.getPlayerTwoChoice(), session.getExpiresAt(),
        session.getResultMatchId() != null ? session.getResultMatchId().value().toString() : null);
  }

}
