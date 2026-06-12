package com.villo.truco.application.ports;

import com.villo.truco.domain.shared.valueobjects.MatchId;

public interface RematchVeto {

  boolean vetoesRematch(MatchId matchId);

}
