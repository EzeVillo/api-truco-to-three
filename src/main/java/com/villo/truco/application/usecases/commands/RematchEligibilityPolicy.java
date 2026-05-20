package com.villo.truco.application.usecases.commands;

import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.Objects;

public final class RematchEligibilityPolicy {

  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;

  public RematchEligibilityPolicy(final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
  }

  public boolean isCasualMatch(final MatchId matchId) {

    return leagueQueryRepository.findByMatchId(matchId).isEmpty()
        && cupQueryRepository.findByMatchId(matchId).isEmpty();
  }

}
