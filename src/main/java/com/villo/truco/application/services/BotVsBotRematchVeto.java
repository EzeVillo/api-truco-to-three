package com.villo.truco.application.services;

import com.villo.truco.application.ports.RematchVeto;
import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.Objects;

public final class BotVsBotRematchVeto implements RematchVeto {

  private final BotVsBotMatchRegistry botVsBotMatchRegistry;

  public BotVsBotRematchVeto(final BotVsBotMatchRegistry botVsBotMatchRegistry) {

    this.botVsBotMatchRegistry = Objects.requireNonNull(botVsBotMatchRegistry);
  }

  @Override
  public boolean vetoesRematch(final MatchId matchId) {

    return this.botVsBotMatchRegistry.isBotVsBotMatch(matchId);
  }

}
