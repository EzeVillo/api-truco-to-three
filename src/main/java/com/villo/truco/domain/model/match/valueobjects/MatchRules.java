package com.villo.truco.domain.model.match.valueobjects;

import com.villo.truco.domain.model.match.exceptions.InvalidMatchRulesException;

public record MatchRules(int gamesToWin, int pointsToWinGame) {

    private static final int DEFAULT_GAMES_TO_WIN = 3;
    private static final int DEFAULT_POINTS_TO_WIN_GAME = 3;

    public MatchRules {

        if (gamesToWin <= 0) {
            throw new InvalidMatchRulesException("gamesToWin must be greater than zero");
        }

        if (pointsToWinGame <= 0) {
            throw new InvalidMatchRulesException("pointsToWinGame must be greater than zero");
        }
    }

    public static MatchRules defaultRules() {

        return new MatchRules(DEFAULT_GAMES_TO_WIN, DEFAULT_POINTS_TO_WIN_GAME);
    }

}
