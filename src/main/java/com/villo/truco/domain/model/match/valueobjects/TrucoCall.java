package com.villo.truco.domain.model.match.valueobjects;

public enum TrucoCall {
    TRUCO, RETRUCO, VALE_CUATRO;

    public int pointsIfAccepted() {

        return switch (this) {
            case TRUCO -> 2;
            case RETRUCO -> 3;
            case VALE_CUATRO -> 4;
        };
    }

    public int pointsIfRejected() {

        return switch (this) {
            case TRUCO -> 1;
            case RETRUCO -> 2;
            case VALE_CUATRO -> 3;
        };
    }

    public TrucoCall next() {

        return switch (this) {
            case TRUCO -> RETRUCO;
            case RETRUCO -> VALE_CUATRO;
            case VALE_CUATRO -> throw new IllegalStateException("No hay siguiente al vale cuatro");
        };
    }

    public boolean hasNext() {

        return this != VALE_CUATRO;
    }
}
