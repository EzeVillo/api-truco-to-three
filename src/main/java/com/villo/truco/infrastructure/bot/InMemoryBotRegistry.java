package com.villo.truco.infrastructure.bot;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryBotRegistry implements BotRegistry {

  private final Map<PlayerId, BotProfile> profiles = new ConcurrentHashMap<>();

  @Override
  public boolean isBot(final PlayerId playerId) {

    return this.profiles.containsKey(playerId);
  }

  @Override
  public Optional<BotProfile> getProfile(final PlayerId playerId) {

    return Optional.ofNullable(this.profiles.get(playerId));
  }

  @Override
  public List<BotProfile> getAll() {

    return new ArrayList<>(this.profiles.values());
  }

  @Override
  public void register(final BotProfile profile) {

    this.profiles.put(profile.playerId(), profile);
  }

}
