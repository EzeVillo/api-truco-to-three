package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.cup.exceptions.PlayerAlreadyInWaitingCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerBusyInCupException;
import com.villo.truco.domain.model.league.exceptions.PlayerAlreadyInWaitingLeagueException;
import com.villo.truco.domain.model.league.exceptions.PlayerBusyInLeagueException;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.model.quickmatch.exceptions.PlayerAlreadyInQueueException;
import com.villo.truco.domain.model.rematch.exceptions.PlayerHasOpenRematchSessionException;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;
import java.util.Optional;

public final class PlayerAvailabilityChecker {

  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final BotRegistry botRegistry;
  private final RematchSessionRepository rematchSessionRepository;
  private final QuickMatchQueuePort quickMatchQueuePort;
  public PlayerAvailabilityChecker(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository, final BotRegistry botRegistry,
      final RematchSessionRepository rematchSessionRepository,
      final QuickMatchQueuePort quickMatchQueuePort) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.rematchSessionRepository = Objects.requireNonNull(rematchSessionRepository);
    this.quickMatchQueuePort = Objects.requireNonNull(quickMatchQueuePort);
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
    IN_MATCH, OPEN_REMATCH, IN_QUICK_QUEUE, IN_LEAGUE, IN_CUP
  }

}
