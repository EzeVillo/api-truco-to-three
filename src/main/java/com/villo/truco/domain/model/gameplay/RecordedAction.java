package com.villo.truco.domain.model.gameplay;

import java.util.Objects;

/**
 * Acción jugable concreta registrada. El {@code detail} contiene el dato mínimo de la acción (carta
 * jugada, canto o respuesta) según el {@link RecordedActionType}; es {@code null} para acciones sin
 * parámetro (p. ej. {@code FOLD} o {@code CALL_TRUCO}). La infraestructura lo serializa a JSONB.
 */
public record RecordedAction(RecordedActionType type, Object detail) {

  public RecordedAction {

    Objects.requireNonNull(type, "type is required");
  }

}
