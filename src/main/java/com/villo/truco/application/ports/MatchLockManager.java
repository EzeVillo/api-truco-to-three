package com.villo.truco.application.ports;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import java.util.function.Supplier;

public interface MatchLockManager {

  <T> T executeWithLock(MatchId matchId, Supplier<T> action);

}
