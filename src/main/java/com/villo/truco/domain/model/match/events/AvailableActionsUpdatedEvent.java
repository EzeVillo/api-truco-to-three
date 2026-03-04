package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.List;

public final class AvailableActionsUpdatedEvent extends DomainEventBase implements
    SeatTargetedEvent {

  private final PlayerSeat seat;
  private final List<AvailableAction> availableActions;

  public AvailableActionsUpdatedEvent(final PlayerSeat seat,
      final List<AvailableAction> availableActions) {

    super("AVAILABLE_ACTIONS_UPDATED");
    this.seat = seat;
    this.availableActions = List.copyOf(availableActions);
  }

  @Override
  public PlayerSeat getTargetSeat() {

    return this.seat;
  }

  public PlayerSeat getSeat() {

    return this.seat;
  }

  public List<AvailableAction> getAvailableActions() {

    return this.availableActions;
  }

}
