package com.villo.truco.domain.model.bot.valueobjects;

public sealed interface BotAction permits BotAction.PlayCard, BotAction.CallTruco,
    BotAction.RespondTruco, BotAction.CallEnvido, BotAction.RespondEnvido, BotAction.Fold {

  record PlayCard(BotCard card) implements BotAction {

  }

  record CallTruco(BotTrucoCall call) implements BotAction {

  }

  record RespondTruco(BotTrucoResponse response) implements BotAction {

  }

  record CallEnvido(BotEnvidoCall call) implements BotAction {

  }

  record RespondEnvido(BotEnvidoResponse response) implements BotAction {

  }

  record Fold() implements BotAction {

  }

}
