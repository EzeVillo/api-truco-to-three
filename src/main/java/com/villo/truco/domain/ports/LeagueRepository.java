package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.league.League;

public interface LeagueRepository {

  void save(League league);

}
