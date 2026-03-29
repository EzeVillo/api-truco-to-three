package com.villo.truco.domain.model.match.valueobjects;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public record PlayedHandInfo(Card cardPlayerOne, Card cardPlayerTwo, PlayerId winner) {

}