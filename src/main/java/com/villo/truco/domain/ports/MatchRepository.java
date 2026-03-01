package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.Match;

public interface MatchRepository {

    void save(Match match);

}
