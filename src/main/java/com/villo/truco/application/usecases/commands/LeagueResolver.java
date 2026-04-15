package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.exceptions.LeagueNotFoundException;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import java.util.Objects;

public final class LeagueResolver {

  private final LeagueQueryRepository leagueQueryRepository;

  public LeagueResolver(final LeagueQueryRepository leagueQueryRepository) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
  }

  public League resolve(final LeagueId leagueId) {

    return this.leagueQueryRepository.findById(leagueId)
        .orElseThrow(() -> new LeagueNotFoundException(leagueId));
  }

}
