package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.dto.JoinResourceDTO;
import com.villo.truco.application.exceptions.JoinCodeNotFoundException;
import com.villo.truco.application.exceptions.PublicCupLobbyConflictException;
import com.villo.truco.application.exceptions.PublicLeagueLobbyConflictException;
import com.villo.truco.application.exceptions.PublicMatchLobbyConflictException;
import com.villo.truco.domain.model.cup.exceptions.PublicCupLobbyUnavailableException;
import com.villo.truco.domain.model.cup.valueobjects.BoutPairing;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.exceptions.PublicLeagueLobbyUnavailableException;
import com.villo.truco.domain.model.league.valueobjects.FixtureActivation;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.exceptions.PublicMatchLobbyUnavailableException;
import com.villo.truco.domain.ports.CupEventNotifier;
import com.villo.truco.domain.ports.CupRepository;
import com.villo.truco.domain.ports.JoinCodeRegistryQueryRepository;
import com.villo.truco.domain.ports.LeagueEventNotifier;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.JoinCodeRegistration;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class JoinTargetDispatcher {

  private final JoinCodeRegistryQueryRepository joinCodeRegistryQueryRepository;
  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final LeagueResolver leagueResolver;
  private final LeagueRepository leagueRepository;
  private final LeagueEventNotifier leagueEventNotifier;
  private final CupResolver cupResolver;
  private final CupRepository cupRepository;
  private final CupEventNotifier cupEventNotifier;
  private final MatchRepository derivedMatchRepository;
  private final PlayerAvailabilityChecker playerAvailabilityChecker;

  public JoinTargetDispatcher(final JoinCodeRegistryQueryRepository joinCodeRegistryQueryRepository,
      final MatchResolver matchResolver, final MatchRepository matchRepository,
      final MatchEventNotifier matchEventNotifier, final LeagueResolver leagueResolver,
      final LeagueRepository leagueRepository, final LeagueEventNotifier leagueEventNotifier,
      final CupResolver cupResolver, final CupRepository cupRepository,
      final CupEventNotifier cupEventNotifier, final MatchRepository derivedMatchRepository,
      final PlayerAvailabilityChecker playerAvailabilityChecker) {

    this.joinCodeRegistryQueryRepository = Objects.requireNonNull(joinCodeRegistryQueryRepository);
    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.leagueResolver = Objects.requireNonNull(leagueResolver);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
    this.leagueEventNotifier = Objects.requireNonNull(leagueEventNotifier);
    this.cupResolver = Objects.requireNonNull(cupResolver);
    this.cupRepository = Objects.requireNonNull(cupRepository);
    this.cupEventNotifier = Objects.requireNonNull(cupEventNotifier);
    this.derivedMatchRepository = Objects.requireNonNull(derivedMatchRepository);
    this.playerAvailabilityChecker = Objects.requireNonNull(playerAvailabilityChecker);
  }

  public JoinResourceDTO joinByCode(final PlayerId playerId, final JoinCode joinCode) {

    this.playerAvailabilityChecker.ensureAvailable(playerId);

    final var registration = this.joinCodeRegistryQueryRepository.findByJoinCode(joinCode)
        .orElseThrow(() -> new JoinCodeNotFoundException(joinCode));

    return this.joinByRegistration(playerId, registration);
  }

  public void joinByTarget(final PlayerId playerId, final JoinTargetType targetType,
      final String targetId) {

    this.playerAvailabilityChecker.ensureAvailable(playerId);

    this.joinByTargetId(playerId, targetType, UUID.fromString(targetId));
  }

  private JoinResourceDTO joinByRegistration(final PlayerId playerId,
      final JoinCodeRegistration registration) {

    return this.joinByTargetId(playerId, registration.targetType(), registration.targetId());
  }

  private JoinResourceDTO joinByTargetId(final PlayerId playerId, final JoinTargetType targetType,
      final UUID targetId) {

    return switch (targetType) {
      case MATCH -> this.joinMatch(playerId, new MatchId(targetId));
      case LEAGUE -> this.joinLeague(playerId, new LeagueId(targetId));
      case CUP -> this.joinCup(playerId, new CupId(targetId));
    };
  }

  private JoinResourceDTO joinMatch(final PlayerId playerId, final MatchId matchId) {

    final var match = this.matchResolver.resolve(matchId);

    try {
      match.join(playerId);
    } catch (final PublicMatchLobbyUnavailableException ex) {
      throw new PublicMatchLobbyConflictException();
    }

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();

    return new JoinResourceDTO(JoinTargetType.MATCH.name(), match.getId().value().toString());
  }

  private JoinResourceDTO joinLeague(final PlayerId playerId, final LeagueId leagueId) {

    final var league = this.leagueResolver.resolve(leagueId);
    final List<FixtureActivation> activations;

    try {
      activations = league.join(playerId);
    } catch (final PublicLeagueLobbyUnavailableException ex) {
      throw new PublicLeagueLobbyConflictException();
    }

    LeagueMatchActivationSupport.createAndLinkInitialMatches(league, this.derivedMatchRepository,
        activations);

    this.leagueRepository.save(league);
    this.leagueEventNotifier.publishDomainEvents(league.getLeagueDomainEvents());
    league.clearDomainEvents();

    return new JoinResourceDTO(JoinTargetType.LEAGUE.name(), league.getId().value().toString());
  }

  private JoinResourceDTO joinCup(final PlayerId playerId, final CupId cupId) {

    final var cup = this.cupResolver.resolve(cupId);
    final List<BoutPairing> pairings;

    try {
      pairings = cup.join(playerId);
    } catch (final PublicCupLobbyUnavailableException ex) {
      throw new PublicCupLobbyConflictException();
    }

    CupMatchActivationSupport.createAndLinkMatches(cup, this.derivedMatchRepository, pairings);

    this.cupRepository.save(cup);
    this.cupEventNotifier.publishDomainEvents(cup.getCupDomainEvents());
    cup.clearDomainEvents();

    return new JoinResourceDTO(JoinTargetType.CUP.name(), cup.getId().value().toString());
  }

}
