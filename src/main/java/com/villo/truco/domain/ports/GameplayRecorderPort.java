package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.gameplay.valueobjects.RecordedDecision;

public interface GameplayRecorderPort {

  void record(RecordedDecision decision);

}
