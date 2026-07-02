package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.exceptions.PlayerAlreadyInWaitingCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerBusyInCupException;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.exceptions.PlayerAlreadyInWaitingLeagueException;
import com.villo.truco.domain.model.league.exceptions.PlayerBusyInLeagueException;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.model.match.exceptions.PlayerOwnsActiveBotMatchException;
import com.villo.truco.domain.model.quickmatch.exceptions.PlayerAlreadyInQueueException;
import com.villo.truco.domain.model.rematch.exceptions.PlayerHasOpenRematchSessionException;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.model.spectator.exceptions.PlayerIsSpectatingException;
import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.ports.SpectatorshipRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class PlayerAvailabilityChecker {

  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final BotRegistry botRegistry;
  private final RematchSessionRepository rematchSessionRepository;
  private final QuickMatchQueuePort quickMatchQueuePort;
  private final SpectatorshipRepository spectatorshipRepository;
  private final BotVsBotMatchRegistry botVsBotMatchRegistry;

  public PlayerAvailabilityChecker(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository, final BotRegistry botRegistry,
      final RematchSessionRepository rematchSessionRepository,
      final QuickMatchQueuePort quickMatchQueuePort,
      final SpectatorshipRepository spectatorshipRepository,
      final BotVsBotMatchRegistry botVsBotMatchRegistry) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.rematchSessionRepository = Objects.requireNonNull(rematchSessionRepository);
    this.quickMatchQueuePort = Objects.requireNonNull(quickMatchQueuePort);
    this.spectatorshipRepository = Objects.requireNonNull(spectatorshipRepository);
    this.botVsBotMatchRegistry = Objects.requireNonNull(botVsBotMatchRegistry);
  }

  private static boolean isBlockedByLeague(final PlayerId playerId,
      final Map<PlayerId, League> inProgressLeagues, final Set<PlayerId> waitingLeague) {

    final var league = inProgressLeagues.get(playerId);
    return (league != null && league.hasPlayerPendingFixtures(playerId)) || waitingLeague.contains(
        playerId);
  }

  private static boolean isBlockedByCup(final PlayerId playerId,
      final Map<PlayerId, Cup> inProgressCups, final Set<PlayerId> waitingCup) {

    final var cup = inProgressCups.get(playerId);
    return (cup != null && cup.isPlayerStillCompeting(playerId)) || waitingCup.contains(playerId);
  }

  public void ensureAvailable(final PlayerId playerId) {

    final var blockingReason = this.findBlockingReason(playerId);
    if (blockingReason.isEmpty()) {
      return;
    }

    switch (blockingReason.get()) {
      case IN_MATCH -> {
        throw new PlayerAlreadyInActiveMatchException();
      }
      case OPEN_REMATCH -> {
        throw new PlayerHasOpenRematchSessionException();
      }
      case IN_QUICK_QUEUE -> {
        throw new PlayerAlreadyInQueueException();
      }
      case SPECTATING -> {
        throw new PlayerIsSpectatingException();
      }
      case OWNS_BOT_MATCH -> {
        throw new PlayerOwnsActiveBotMatchException();
      }
      case IN_LEAGUE -> this.throwLeagueBusy(playerId);
      case IN_CUP -> this.throwCupBusy(playerId);
    }
  }

  public Optional<BlockingReason> findBlockingReason(final PlayerId playerId) {

    if (this.botRegistry.isBot(playerId)) {
      return Optional.empty();
    }

    if (this.matchQueryRepository.hasUnfinishedMatch(playerId)) {
      return Optional.of(BlockingReason.IN_MATCH);
    }

    if (this.rematchSessionRepository.findOpenByPlayer(playerId).isPresent()) {
      return Optional.of(BlockingReason.OPEN_REMATCH);
    }

    if (this.quickMatchQueuePort.isPlayerQueued(playerId)) {
      return Optional.of(BlockingReason.IN_QUICK_QUEUE);
    }

    if (this.spectatorshipRepository.findBySpectatorId(playerId).filter(s -> s.isActive())
        .isPresent()) {
      return Optional.of(BlockingReason.SPECTATING);
    }

    if (this.botVsBotMatchRegistry.findActiveOwnedMatchId(playerId).isPresent()) {
      return Optional.of(BlockingReason.OWNS_BOT_MATCH);
    }

    if (this.leagueQueryRepository.findInProgressByPlayer(playerId)
        .filter(league -> league.hasPlayerPendingFixtures(playerId)).isPresent()
        || this.leagueQueryRepository.findWaitingByPlayer(playerId).isPresent()) {
      return Optional.of(BlockingReason.IN_LEAGUE);
    }

    if (this.cupQueryRepository.findInProgressByPlayer(playerId)
        .filter(cup -> cup.isPlayerStillCompeting(playerId)).isPresent()
        || this.cupQueryRepository.findWaitingByPlayer(playerId).isPresent()) {
      return Optional.of(BlockingReason.IN_CUP);
    }

    return Optional.empty();
  }

  public Map<PlayerId, BlockingReason> findBlockingReasonsFor(final Set<PlayerId> playerIds) {

    final var candidates = playerIds.stream().filter(playerId -> !this.botRegistry.isBot(playerId))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (candidates.isEmpty()) {
      return Map.of();
    }

    final var inMatch = this.matchQueryRepository.findPlayersWithUnfinishedMatch(candidates);
    final var openRematch = this.rematchSessionRepository.findPlayersWithOpenRematch(candidates);
    final var ownsBotMatch = this.botVsBotMatchRegistry.findOwnersWithActiveMatch(candidates);
    final var inProgressLeagues = this.leagueQueryRepository.findInProgressByPlayers(candidates);
    final var waitingLeague = this.leagueQueryRepository.findPlayersWaitingInLeague(candidates);
    final var inProgressCups = this.cupQueryRepository.findInProgressByPlayers(candidates);
    final var waitingCup = this.cupQueryRepository.findPlayersWaitingInCup(candidates);

    final var reasons = new LinkedHashMap<PlayerId, BlockingReason>();
    for (final var playerId : candidates) {
      final var reason = this.resolveBlockingReason(playerId, inMatch, openRematch, ownsBotMatch,
          inProgressLeagues, waitingLeague, inProgressCups, waitingCup);
      if (reason != null) {
        reasons.put(playerId, reason);
      }
    }
    return reasons;
  }

  private BlockingReason resolveBlockingReason(final PlayerId playerId, final Set<PlayerId> inMatch,
      final Set<PlayerId> openRematch, final Set<PlayerId> ownsBotMatch,
      final Map<PlayerId, League> inProgressLeagues, final Set<PlayerId> waitingLeague,
      final Map<PlayerId, Cup> inProgressCups, final Set<PlayerId> waitingCup) {

    if (inMatch.contains(playerId)) {
      return BlockingReason.IN_MATCH;
    }
    if (openRematch.contains(playerId)) {
      return BlockingReason.OPEN_REMATCH;
    }
    if (this.quickMatchQueuePort.isPlayerQueued(playerId)) {
      return BlockingReason.IN_QUICK_QUEUE;
    }
    if (this.isSpectating(playerId)) {
      return BlockingReason.SPECTATING;
    }
    if (ownsBotMatch.contains(playerId)) {
      return BlockingReason.OWNS_BOT_MATCH;
    }
    if (isBlockedByLeague(playerId, inProgressLeagues, waitingLeague)) {
      return BlockingReason.IN_LEAGUE;
    }
    if (isBlockedByCup(playerId, inProgressCups, waitingCup)) {
      return BlockingReason.IN_CUP;
    }
    return null;
  }

  private boolean isSpectating(final PlayerId playerId) {

    return this.spectatorshipRepository.findBySpectatorId(playerId).filter(Spectatorship::isActive)
        .isPresent();
  }

  public void ensureCanStartMatch(final PlayerId playerId) {

    if (this.botRegistry.isBot(playerId)) {
      return;
    }

    if (this.matchQueryRepository.hasActiveMatch(playerId)) {
      throw new PlayerAlreadyInActiveMatchException();
    }
    this.ensureNotInWaitingTournament(playerId);
  }

  private void ensureNotInWaitingTournament(final PlayerId playerId) {

    this.leagueQueryRepository.findWaitingByPlayer(playerId).ifPresent(league -> {
      throw new PlayerAlreadyInWaitingLeagueException();
    });

    this.cupQueryRepository.findWaitingByPlayer(playerId).ifPresent(cup -> {
      throw new PlayerAlreadyInWaitingCupException();
    });
  }

  private void throwLeagueBusy(final PlayerId playerId) {

    this.leagueQueryRepository.findInProgressByPlayer(playerId)
        .filter(league -> league.hasPlayerPendingFixtures(playerId)).ifPresent(league -> {
          throw new PlayerBusyInLeagueException();
        });
    throw new PlayerAlreadyInWaitingLeagueException();
  }

  private void throwCupBusy(final PlayerId playerId) {

    this.cupQueryRepository.findInProgressByPlayer(playerId)
        .filter(cup -> cup.isPlayerStillCompeting(playerId)).ifPresent(cup -> {
          throw new PlayerBusyInCupException();
        });
    throw new PlayerAlreadyInWaitingCupException();
  }

  public enum BlockingReason {
    IN_MATCH, OPEN_REMATCH, IN_QUICK_QUEUE, SPECTATING, OWNS_BOT_MATCH, IN_LEAGUE, IN_CUP
  }

}
