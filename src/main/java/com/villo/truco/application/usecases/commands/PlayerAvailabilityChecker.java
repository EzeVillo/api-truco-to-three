package com.villo.truco.application.usecases.commands;

import com.villo.truco.domain.model.cup.exceptions.PlayerBusyInCupException;
import com.villo.truco.domain.model.league.exceptions.PlayerBusyInLeagueException;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class PlayerAvailabilityChecker {

  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;

  public PlayerAvailabilityChecker(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
  }

  public void ensureAvailable(final PlayerId playerId) {

    if (this.matchQueryRepository.hasUnfinishedMatch(playerId)) {
      throw new PlayerAlreadyInActiveMatchException();
    }

    ensureNoActiveTournaments(playerId);
  }

  public void ensureNoActiveTournaments(final PlayerId playerId) {

    this.leagueQueryRepository.findInProgressByPlayer(playerId)
        .filter(league -> league.hasPlayerPendingFixtures(playerId)).ifPresent(league -> {
          throw new PlayerBusyInLeagueException();
        });

    this.cupQueryRepository.findInProgressByPlayer(playerId)
        .filter(cup -> cup.isPlayerStillCompeting(playerId)).ifPresent(cup -> {
          throw new PlayerBusyInCupException();
        });
  }

}
