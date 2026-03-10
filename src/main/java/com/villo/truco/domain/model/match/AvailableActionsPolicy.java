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
      final PlayerId currentTurn, final TrucoStateMachine trucoStateMachine,
      final EnvidoStateMachine envidoStateMachine, final boolean isFirstHand,
      final boolean hasPlayerPlayedInCurrentHand, final boolean isMano) {

    if (status == RoundStatus.FINISHED) {
      return List.of();
    }

    if (!currentTurn.equals(playerId)) {
      return List.of();
    }

    final var actions = new ArrayList<AvailableAction>();

    if (status == RoundStatus.PLAYING) {
      actions.add(AvailableAction.of(ActionType.PLAY_CARD));
      if (canFold(trucoStateMachine, envidoStateMachine, isFirstHand, isMano)) {
        actions.add(AvailableAction.of(ActionType.FOLD));
      }
      addTrucoActions(playerId, trucoStateMachine, actions);
      addEnvidoActions(status, hasPlayerPlayedInCurrentHand, trucoStateMachine, envidoStateMachine,
          isFirstHand, actions);
    }

    if (status == RoundStatus.TRUCO_IN_PROGRESS) {
      actions.add(AvailableAction.of(ActionType.RESPOND_TRUCO, TrucoResponse.QUIERO.name()));
      actions.add(AvailableAction.of(ActionType.RESPOND_TRUCO, TrucoResponse.NO_QUIERO.name()));
      actions.add(AvailableAction.of(ActionType.RESPOND_TRUCO,
          TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO.name()));

      addEnvidoActions(status, hasPlayerPlayedInCurrentHand, trucoStateMachine, envidoStateMachine,
          isFirstHand, actions);

      if (trucoStateMachine.getCurrentCall().hasNext()) {
        actions.add(AvailableAction.of(ActionType.CALL_TRUCO,
            trucoStateMachine.getCurrentCall().next().name()));
      }
    }

    if (status == RoundStatus.ENVIDO_IN_PROGRESS) {
      actions.add(AvailableAction.of(ActionType.RESPOND_ENVIDO, EnvidoResponse.QUIERO.name()));
      actions.add(AvailableAction.of(ActionType.RESPOND_ENVIDO, EnvidoResponse.NO_QUIERO.name()));
      addEnvidoRaiseActions(envidoStateMachine, actions);
    }

    return actions;
  }

  private static void addTrucoActions(final PlayerId playerId,
      final TrucoStateMachine trucoStateMachine, final List<AvailableAction> actions) {

    if (!trucoStateMachine.hasBeenCalled()) {
      actions.add(AvailableAction.of(ActionType.CALL_TRUCO, TrucoCall.TRUCO.name()));
    } else if (trucoStateMachine.canEscalate(playerId)) {
      actions.add(AvailableAction.of(ActionType.CALL_TRUCO,
          trucoStateMachine.getCurrentCall().next().name()));
    }
  }

  private static boolean canFold(final TrucoStateMachine trucoStateMachine,
      final EnvidoStateMachine envidoStateMachine, final boolean isFirstHand,
      final boolean isMano) {

    return FoldAllowedSpecification.isSatisfiedBy(isMano, isFirstHand,
        envidoStateMachine.isResolved(), trucoStateMachine.hasBeenCalled());
  }

  private static void addEnvidoActions(final RoundStatus status,
      final boolean hasPlayerPlayedInCurrentHand, final TrucoStateMachine trucoStateMachine,
      final EnvidoStateMachine envidoStateMachine, final boolean isFirstHand,
      final List<AvailableAction> actions) {

    final var decision = EnvidoCallSpecification.evaluate(status, isFirstHand,
        hasPlayerPlayedInCurrentHand, envidoStateMachine.isResolved(),
        trucoStateMachine.hasBeenCalled(), trucoStateMachine.getCurrentCall());

    if (!decision.satisfied() || !envidoStateMachine.isEmpty()) {
      return;
    }

    actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, EnvidoCall.ENVIDO.name()));
    actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, EnvidoCall.REAL_ENVIDO.name()));
    actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, EnvidoCall.FALTA_ENVIDO.name()));
  }

  private static void addEnvidoRaiseActions(final EnvidoStateMachine envidoStateMachine,
      final List<AvailableAction> actions) {

    for (final var call : EnvidoCall.values()) {
      if (envidoStateMachine.canRaiseWith(call)) {
        actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, call.name()));
      }
    }
  }

}
