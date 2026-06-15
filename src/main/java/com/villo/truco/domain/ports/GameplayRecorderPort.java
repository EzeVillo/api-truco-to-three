package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.gameplay.RecordedDecision;

/**
 * Puerto de salida para persistir, de forma append-only e idempotente por {@code (matchId,
 * stateVersion)}, cada decisión jugable de una partida. Las excepciones pueden propagar; el
 * decorator de registro las traga y loguea para no afectar la jugada (FR-010).
 */
public interface GameplayRecorderPort {

  void record(RecordedDecision decision);

}
