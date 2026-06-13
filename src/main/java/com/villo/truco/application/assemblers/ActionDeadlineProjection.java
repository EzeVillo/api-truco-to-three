package com.villo.truco.application.assemblers;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;

/**
 * Proyección del temporizador de turno: {@code actionDeadline = lastActivityAt + idleTimeout}.
 *
 * <p>Fuente única de la fórmula (INV-1): la usan tanto el snapshot del jugador como el del
 * espectador. Si ningún asiento debe actuar, los tres campos quedan en {@code null} (INV-3).
 */
record ActionDeadlineProjection(Long actionDeadline, Long turnDurationMillis,
                                String actionDeadlineSeat) {

  static ActionDeadlineProjection of(final Match match, final long idleTimeoutMillis) {

    final var currentTurn = match.getCurrentTurn();
    if (!match.forfeitsOnInactivity() || currentTurn == null || match.getLastActivityAt() == null) {
      return new ActionDeadlineProjection(null, null, null);
    }

    final var seat =
        currentTurn.equals(match.getPlayerOne()) ? PlayerSeat.PLAYER_ONE : PlayerSeat.PLAYER_TWO;
    return new ActionDeadlineProjection(
        match.getLastActivityAt().toEpochMilli() + idleTimeoutMillis, idleTimeoutMillis,
        seat.name());
  }

}
