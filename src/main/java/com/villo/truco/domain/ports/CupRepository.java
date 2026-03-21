package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.cup.Cup;

public interface CupRepository {

  void save(Cup cup);

}
