package com.villo.truco.domain.model.gameplay;

import com.villo.truco.domain.model.match.MatchSnapshot;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.time.Instant;
import java.util.Objects;

/**
 * Decisión jugable registrada de forma append-only. Contiene el estado completo y sin redactar de la
 * partida tras la transición ({@code snapshot}) junto con la acción que la produjo y quién la tomó.
 * Es un contenedor de datos para el puerto de salida; no contiene lógica de juego.
 */
public record RecordedDecision(MatchId matchId, long stateVersion, int gameNumber, int roundNumber,
                               ActorSeat actorSeat, ActorType actorType, RecordedAction action,
                               MatchSnapshot snapshot, Instant occurredAt, int schemaVersion) {

  public RecordedDecision {

    Objects.requireNonNull(matchId, "matchId is required");
    Objects.requireNonNull(actorSeat, "actorSeat is required");
    Objects.requireNonNull(actorType, "actorType is required");
    Objects.requireNonNull(action, "action is required");
    Objects.requireNonNull(snapshot, "snapshot is required");
    Objects.requireNonNull(occurredAt, "occurredAt is required");
  }

}
