package com.villo.truco.campaign.infrastructure.bot;

import com.villo.truco.application.ports.BotRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public final class CampaignBotCatalogInitializer implements ApplicationRunner {

  private final BotRegistry botRegistry;
  private final CampaignBotCatalog campaignBotCatalog;

  public CampaignBotCatalogInitializer(final BotRegistry botRegistry,
      final CampaignBotCatalog campaignBotCatalog) {

    this.botRegistry = botRegistry;
    this.campaignBotCatalog = campaignBotCatalog;
  }

  @Override
  public void run(final ApplicationArguments args) {

    this.campaignBotCatalog.botProfiles().forEach(this.botRegistry::register);
  }

}
