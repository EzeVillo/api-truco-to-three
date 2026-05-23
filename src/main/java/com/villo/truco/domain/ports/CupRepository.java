package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.cup.Cup;
import java.util.stream.Stream;

public interface CupRepository {

  void save(Cup cup);

  Stream<CupTimeoutEntry> findActiveWithTimeoutDeadline();

}
