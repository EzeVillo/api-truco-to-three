package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinPublicCupCommand;
import com.villo.truco.application.dto.JoinCupDTO;
import com.villo.truco.application.exceptions.PublicCupLobbyConflictException;
import com.villo.truco.application.ports.in.JoinPublicCupUseCase;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.exceptions.PublicCupLobbyUnavailableException;
import com.villo.truco.domain.model.cup.valueobjects.BoutPairing;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class JoinPublicCupCommandHandler implements JoinPublicCupUseCase {

  private final CupResolver cupResolver;
  private final CupRepository cupRepository;
  private final MatchRepository matchRepository;
  private final CupEventNotifier cupEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public JoinPublicCupCommandHandler(final CupResolver cupResolver,
      final CupRepository cupRepository, final MatchRepository matchRepository,
      final CupEventNotifier cupEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.cupResolver = Objects.requireNonNull(cupResolver);
    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.cupEventNotifier = Objects.requireNonNull(cupEventNotifier);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  private static List<BoutPairing> tryJoinPublic(final Cup cup, final PlayerId playerId) {

    try {
      return cup.joinPublic(playerId);
    } catch (final PublicCupLobbyUnavailableException ex) {
      throw new PublicCupLobbyConflictException();
    }
  }

  @Override
  public JoinCupDTO handle(final JoinPublicCupCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var cup = this.cupResolver.resolve(command.cupId());
    final var pairings = tryJoinPublic(cup, command.playerId());

    CupMatchActivationSupport.createAndLinkMatches(cup, this.matchRepository, pairings);

    this.cupRepository.save(cup);
    this.cupEventNotifier.publishDomainEvents(cup.getCupDomainEvents());
    cup.clearDomainEvents();

    return new JoinCupDTO(cup.getId().value().toString());
  }

}
