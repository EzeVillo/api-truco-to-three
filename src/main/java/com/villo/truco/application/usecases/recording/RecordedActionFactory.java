package com.villo.truco.application.usecases.recording;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.commands.MatchActionCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.domain.model.gameplay.RecordedAction;
import com.villo.truco.domain.model.gameplay.RecordedActionType;

/**
 * Mapea cada uno de los 6 commands de acción a su {@link RecordedAction} (tipo + detalle mínimo). El
 * detalle es {@code null} para las acciones sin parámetro ({@code CALL_TRUCO}, {@code FOLD}).
 */
public final class RecordedActionFactory {

  public RecordedAction from(final MatchActionCommand command) {

    return switch (command) {
      case PlayCardCommand c -> new RecordedAction(RecordedActionType.PLAY_CARD, c.card());
      case CallTrucoCommand ignored -> new RecordedAction(RecordedActionType.CALL_TRUCO, null);
      case RespondTrucoCommand c -> new RecordedAction(RecordedActionType.RESPOND_TRUCO,
          c.response());
      case CallEnvidoCommand c -> new RecordedAction(RecordedActionType.CALL_ENVIDO, c.call());
      case RespondEnvidoCommand c -> new RecordedAction(RecordedActionType.RESPOND_ENVIDO,
          c.response());
      case FoldCommand ignored -> new RecordedAction(RecordedActionType.FOLD, null);
      default -> throw new IllegalArgumentException(
          "Command de acción no soportado: " + command.getClass().getName());
    };
  }

}
