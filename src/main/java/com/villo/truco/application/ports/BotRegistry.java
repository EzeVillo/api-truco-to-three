package com.villo.truco.application.ports;

import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Optional;

public interface BotRegistry {

  boolean isBot(PlayerId playerId);

  Optional<BotProfile> getProfile(PlayerId playerId);

  List<BotProfile> getAll();

  void register(BotProfile profile);

}
