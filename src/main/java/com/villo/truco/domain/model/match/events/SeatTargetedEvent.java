package com.villo.truco.domain.model.match.events;

import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;

public interface SeatTargetedEvent {

  PlayerSeat getTargetSeat();

}
