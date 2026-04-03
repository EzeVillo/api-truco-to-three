package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.assemblers.SpectatorMatchStateDTOAssembler;
import com.villo.truco.application.commands.SpectateMatchCommand;
import com.villo.truco.application.dto.SpectatorMatchStateDTO;
import com.villo.truco.application.exceptions.MatchNotFoundException;
import com.villo.truco.application.ports.in.SpectateMatchUseCase;
import com.villo.truco.domain.model.spectator.SpectatingEligibilityPolicy;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import java.util.Objects;

public final class SpectateMatchCommandHandler implements SpectateMatchUseCase {

  private final MatchQueryRepository matchQueryRepository;
  private final SpectatorshipRepository spectatorshipRepository;
  private final SpectatingEligibilityPolicy eligibilityPolicy;
  private final SpectatorCountChangedPublisher countChangedPublisher;
  private final SpectatorMatchStateDTOAssembler dtoAssembler;

  public SpectateMatchCommandHandler(final MatchQueryRepository matchQueryRepository,
      final SpectatorshipRepository spectatorshipRepository,
      final SpectatingEligibilityPolicy eligibilityPolicy,
      final SpectatorCountChangedPublisher countChangedPublisher,
      final SpectatorMatchStateDTOAssembler dtoAssembler) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.spectatorshipRepository = Objects.requireNonNull(spectatorshipRepository);
    this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy);
    this.countChangedPublisher = Objects.requireNonNull(countChangedPublisher);
    this.dtoAssembler = Objects.requireNonNull(dtoAssembler);
  }

  @Override
  public SpectatorMatchStateDTO handle(final SpectateMatchCommand command) {

    final var match = this.matchQueryRepository.findById(command.matchId())
        .orElseThrow(() -> new MatchNotFoundException(command.matchId()));
    final var spectatorship = this.spectatorshipRepository.findBySpectatorId(command.spectatorId())
        .orElseGet(() -> Spectatorship.create(command.spectatorId()));

    this.eligibilityPolicy.ensureCanStartWatching(spectatorship, match);
    spectatorship.startWatching(command.matchId());
    this.spectatorshipRepository.save(spectatorship);
    this.countChangedPublisher.publishFor(command.matchId());

    return this.dtoAssembler.toDto(match,
        this.countChangedPublisher.countByMatch(command.matchId()));
  }

}
