package com.villo.truco.domain.model.bot.valueobjects;

public record BotEnvidoCall(int acceptedPointsIfBotWins, int acceptedPointsIfRivalWins,
                            int rejectedPointsIfRivalDeclines, BotEnvidoLevel level) {

}
