package com.villo.truco.application.dto;

import com.villo.truco.domain.model.match.valueobjects.Card;

public record CardDTO(String suit, int number) {

    public static CardDTO from(final Card card) {

        return new CardDTO(card.suit().name(), card.number());
    }

}
