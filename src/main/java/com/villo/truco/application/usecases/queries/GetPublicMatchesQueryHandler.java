package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.assemblers.PublicMatchLobbyDTOAssembler;
import com.villo.truco.application.dto.PublicMatchLobbyDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetPublicMatchesUseCase;
import com.villo.truco.application.queries.GetPublicMatchesQuery;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import java.util.Objects;

public final class GetPublicMatchesQueryHandler implements GetPublicMatchesUseCase {

  private final MatchQueryRepository matchQueryRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final PublicMatchLobbyDTOAssembler assembler;

  public GetPublicMatchesQueryHandler(final MatchQueryRepository matchQueryRepository,
      final PublicActorResolver publicActorResolver,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
    this.assembler = new PublicMatchLobbyDTOAssembler(Objects.requireNonNull(publicActorResolver));
  }

  @Override
  public CursorPageResult<PublicMatchLobbyDTO> handle(final GetPublicMatchesQuery query) {

    this.playerAvailabilityChecker.ensureAvailable(query.playerId());

    final var matches = this.matchQueryRepository.findPublicWaiting(query.pageQuery());
    return new CursorPageResult<>(this.assembler.assembleAll(matches.items()),
        matches.nextCursor());
  }

}
