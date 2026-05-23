package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.Match;
import java.util.stream.Stream;

public interface MatchRepository {

  void save(Match match);

  Stream<MatchTimeoutEntry> findActiveWithTimeoutDeadline();

}
