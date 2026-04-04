package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.assemblers.PublicLeagueLobbyDTOAssembler;
import com.villo.truco.application.dto.PublicLeagueLobbyDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetPublicLeaguesUseCase;
import com.villo.truco.application.queries.GetPublicLeaguesQuery;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import java.util.Objects;

public final class GetPublicLeaguesQueryHandler implements GetPublicLeaguesUseCase {

  private final LeagueQueryRepository leagueQueryRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final PublicLeagueLobbyDTOAssembler assembler;

  public GetPublicLeaguesQueryHandler(final LeagueQueryRepository leagueQueryRepository,
      final PublicActorResolver publicActorResolver,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
    this.assembler = new PublicLeagueLobbyDTOAssembler(Objects.requireNonNull(publicActorResolver));
  }

  @Override
  public CursorPageResult<PublicLeagueLobbyDTO> handle(final GetPublicLeaguesQuery query) {

    this.playerAvailabilityChecker.ensureAvailable(query.playerId());

    final var leagues = this.leagueQueryRepository.findPublicWaiting(query.pageQuery());
    return new CursorPageResult<>(this.assembler.assembleAll(leagues.items()),
        leagues.nextCursor());
  }

}
