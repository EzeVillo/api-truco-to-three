package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.JoinPublicLeagueCommand;
import com.villo.truco.application.dto.JoinLeagueDTO;
import com.villo.truco.application.exceptions.PublicLeagueLobbyConflictException;
import com.villo.truco.application.ports.in.JoinPublicLeagueUseCase;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.exceptions.PublicLeagueLobbyUnavailableException;
import com.villo.truco.domain.model.league.valueobjects.FixtureActivation;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public final class JoinPublicLeagueCommandHandler implements JoinPublicLeagueUseCase {

  private final LeagueResolver leagueResolver;
  private final LeagueRepository leagueRepository;
  private final MatchRepository matchRepository;
  private final LeagueEventNotifier leagueEventNotifier;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public JoinPublicLeagueCommandHandler(final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository, final MatchRepository matchRepository,
      final LeagueEventNotifier leagueEventNotifier,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.leagueResolver = Objects.requireNonNull(leagueResolver);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.leagueEventNotifier = Objects.requireNonNull(leagueEventNotifier);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  private static List<FixtureActivation> tryJoinPublic(final League league,
      final PlayerId playerId) {

    try {
      return league.joinPublic(playerId);
    } catch (final PublicLeagueLobbyUnavailableException ex) {
      throw new PublicLeagueLobbyConflictException();
    }
  }

  @Override
  public JoinLeagueDTO handle(final JoinPublicLeagueCommand command) {

    this.playerAvailabilityChecker.ensureAvailable(command.playerId());

    final var league = this.leagueResolver.resolve(command.leagueId());
    final var activations = tryJoinPublic(league, command.playerId());

    LeagueMatchActivationSupport.createAndLinkInitialMatches(league, this.matchRepository,
        activations);

    this.leagueRepository.save(league);
    this.leagueEventNotifier.publishDomainEvents(league.getLeagueDomainEvents());
    league.clearDomainEvents();

    return new JoinLeagueDTO(league.getId().value().toString());
  }

}
