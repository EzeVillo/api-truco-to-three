package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.tournament.Tournament;

public interface TournamentRepository {

  void save(Tournament tournament);

}
