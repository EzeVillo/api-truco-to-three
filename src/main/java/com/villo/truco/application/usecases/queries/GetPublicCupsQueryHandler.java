package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.assemblers.PublicCupLobbyDTOAssembler;
import com.villo.truco.application.dto.PublicCupLobbyDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetPublicCupsUseCase;
import com.villo.truco.application.queries.GetPublicCupsQuery;
import com.villo.truco.application.usecases.commands.PlayerAvailabilityChecker;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import java.util.Objects;

public final class GetPublicCupsQueryHandler implements GetPublicCupsUseCase {

  private final CupQueryRepository cupQueryRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;
  private final PublicCupLobbyDTOAssembler assembler;

  public GetPublicCupsQueryHandler(final CupQueryRepository cupQueryRepository,
      final PublicActorResolver publicActorResolver,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
    this.assembler = new PublicCupLobbyDTOAssembler(Objects.requireNonNull(publicActorResolver));
  }

  @Override
  public CursorPageResult<PublicCupLobbyDTO> handle(final GetPublicCupsQuery query) {

    this.playerAvailabilityChecker.ensureAvailable(query.playerId());

    final var cups = this.cupQueryRepository.findPublicWaiting(query.pageQuery());
    return new CursorPageResult<>(this.assembler.assembleAll(cups.items()), cups.nextCursor());
  }

}
