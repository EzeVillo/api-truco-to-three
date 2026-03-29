package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.HandId;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import java.util.List;

public record HandSnapshot(HandId id, List<Card> cards) {

}
