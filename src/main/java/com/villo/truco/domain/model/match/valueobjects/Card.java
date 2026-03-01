package com.villo.truco.domain.model.match.valueobjects;

import com.villo.truco.domain.model.match.valueobjects.exceptions.InvalidCardException;
import java.util.Objects;
import java.util.Set;

public record Card(Suit suit, int number) {

    private static final Set<Integer> VALID_NUMBERS = Set.of(1, 2, 3, 4, 5, 6, 7, 10, 11, 12);

    public static Card of(final Suit suit, final int number) {

        Objects.requireNonNull(suit, "Suit cannot be null");

        if (!VALID_NUMBERS.contains(number)) {
            throw new InvalidCardException(number);
        }

        return new Card(suit, number);
    }

    @Override
    public String toString() {

        return number + " of " + suit;
    }

}
