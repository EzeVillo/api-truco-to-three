package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.league.League;
import java.util.stream.Stream;

public interface LeagueRepository {

  void save(League league);

  Stream<LeagueTimeoutEntry> findActiveWithTimeoutDeadline();

}
