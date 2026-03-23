package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record PlayedHandSnapshot(Card cardMano, Card cardPie, PlayerId winner) {

}
