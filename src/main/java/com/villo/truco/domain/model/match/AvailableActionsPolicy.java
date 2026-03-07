package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.ActionType;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import java.util.ArrayList;
import java.util.List;

final class AvailableActionsPolicy {

  private AvailableActionsPolicy() {

  }

  static List<AvailableAction> resolve(final RoundStatus status, final PlayerId playerId,
      final PlayerId currentTurn, final TrucoFlow trucoFlow, final EnvidoFlow envidoFlow,
      final boolean isFirstHand, final boolean hasPlayerPlayedInCurrentHand) {

    if (status == RoundStatus.FINISHED) {
      return List.of();
    }

    if (!currentTurn.equals(playerId)) {
      return List.of();
    }

    final var actions = new ArrayList<AvailableAction>();

    if (status == RoundStatus.PLAYING) {
      actions.add(AvailableAction.of(ActionType.PLAY_CARD));
      actions.add(AvailableAction.of(ActionType.FOLD));
      addTrucoActions(playerId, trucoFlow, actions);
      if (!trucoFlow.hasBeenCalled()) {
        addEnvidoActions(hasPlayerPlayedInCurrentHand, envidoFlow, isFirstHand, actions);
      }
    }

    if (status == RoundStatus.TRUCO_IN_PROGRESS) {
      actions.add(AvailableAction.of(ActionType.RESPOND_TRUCO, TrucoResponse.QUIERO.name()));
      actions.add(AvailableAction.of(ActionType.RESPOND_TRUCO, TrucoResponse.NO_QUIERO.name()));
      actions.add(AvailableAction.of(ActionType.RESPOND_TRUCO,
          TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO.name()));

      if (trucoFlow.getCurrentCall() == TrucoCall.TRUCO) {
        addEnvidoActions(hasPlayerPlayedInCurrentHand, envidoFlow, isFirstHand, actions);
      }

      if (trucoFlow.getCurrentCall().hasNext()) {
        actions.add(
            AvailableAction.of(ActionType.CALL_TRUCO, trucoFlow.getCurrentCall().next().name()));
      }
    }

    if (status == RoundStatus.ENVIDO_IN_PROGRESS) {
      actions.add(AvailableAction.of(ActionType.RESPOND_ENVIDO, EnvidoResponse.QUIERO.name()));
      actions.add(AvailableAction.of(ActionType.RESPOND_ENVIDO, EnvidoResponse.NO_QUIERO.name()));
      addEnvidoRaiseActions(envidoFlow, actions);
    }

    return actions;
  }

  private static void addTrucoActions(final PlayerId playerId, final TrucoFlow trucoFlow,
      final List<AvailableAction> actions) {

    if (!trucoFlow.hasBeenCalled()) {
      actions.add(AvailableAction.of(ActionType.CALL_TRUCO, TrucoCall.TRUCO.name()));
    } else if (trucoFlow.canEscalate(playerId)) {
      actions.add(
          AvailableAction.of(ActionType.CALL_TRUCO, trucoFlow.getCurrentCall().next().name()));
    }
  }

  private static void addEnvidoActions(final boolean hasPlayerPlayedInCurrentHand,
      final EnvidoFlow envidoFlow, final boolean isFirstHand, final List<AvailableAction> actions) {

    if (!isFirstHand || envidoFlow.isResolved()) {
      return;
    }
    if (hasPlayerPlayedInCurrentHand) {
      return;
    }

    if (envidoFlow.isEmpty()) {
      actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, EnvidoCall.ENVIDO.name()));
      actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, EnvidoCall.REAL_ENVIDO.name()));
      actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, EnvidoCall.FALTA_ENVIDO.name()));
    }
  }

  private static void addEnvidoRaiseActions(final EnvidoFlow envidoFlow,
      final List<AvailableAction> actions) {

    for (final var call : EnvidoCall.values()) {
      if (envidoFlow.canRaiseWith(call)) {
        actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, call.name()));
      }
    }
  }

}
