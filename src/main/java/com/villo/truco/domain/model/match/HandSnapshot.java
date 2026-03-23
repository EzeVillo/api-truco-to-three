package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.HandId;
import java.util.List;

public record HandSnapshot(HandId id, List<Card> cards) {

}
