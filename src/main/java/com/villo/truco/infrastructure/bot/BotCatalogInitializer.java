package com.villo.truco.infrastructure.bot;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public final class BotCatalogInitializer implements ApplicationRunner {

  private final BotRegistry botRegistry;

  public BotCatalogInitializer(final BotRegistry botRegistry) {

    this.botRegistry = botRegistry;
  }

  @Override
  public void run(final ApplicationArguments args) {

    this.botRegistry.register(
        new BotProfile(PlayerId.of("00000000-0000-0000-0000-000000000001"), "El Mentiroso",
            new BotPersonality(90, 20, 70, 50, 30)));

    this.botRegistry.register(
        new BotProfile(PlayerId.of("00000000-0000-0000-0000-000000000002"), "El Pescador",
            new BotPersonality(30, 90, 40, 60, 70)));

    this.botRegistry.register(
        new BotProfile(PlayerId.of("00000000-0000-0000-0000-000000000003"), "El Temerario",
            new BotPersonality(60, 30, 95, 70, 15)));

    this.botRegistry.register(
        new BotProfile(PlayerId.of("00000000-0000-0000-0000-000000000004"), "El Cauteloso",
            new BotPersonality(15, 50, 20, 40, 85)));

    this.botRegistry.register(
        new BotProfile(PlayerId.of("00000000-0000-0000-0000-000000000005"), "El Equilibrado",
            new BotPersonality(50, 50, 50, 50, 50)));
  }

}
