package com.villo.truco.domain.model.match;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record CardPlaySnapshot(PlayerId playerId, Card card) {

}
