package com.villo.truco.campaign.infrastructure.config;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.CreateBotMatchUseCase;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.campaign.application.ports.out.CampaignDomainEventHandler;
import com.villo.truco.campaign.application.services.CampaignChallengeResolutionService;
import com.villo.truco.campaign.application.services.CampaignHiddenBotIdsProvider;
import com.villo.truco.campaign.application.services.CampaignRematchVeto;
import com.villo.truco.campaign.application.services.CampaignRevealedBotIdsProvider;
import com.villo.truco.campaign.application.services.CampaignUserGuard;
import com.villo.truco.campaign.application.usecases.commands.StartCampaignChallengeCommandHandler;
import com.villo.truco.campaign.application.usecases.commands.StartCampaignChallengeUseCase;
import com.villo.truco.campaign.application.usecases.queries.GetCampaignQueryHandler;
import com.villo.truco.campaign.application.usecases.queries.GetCampaignUseCase;
import com.villo.truco.campaign.domain.ports.CampaignEventNotifier;
import com.villo.truco.campaign.domain.ports.CampaignMatchRegistry;
import com.villo.truco.campaign.domain.ports.CampaignProgressRepository;
import com.villo.truco.campaign.infrastructure.bot.CampaignBotCatalog;
import com.villo.truco.campaign.infrastructure.bot.CampaignBotCatalogInitializer;
import com.villo.truco.campaign.infrastructure.eventhandlers.CampaignMatchDomainEventHandler;
import com.villo.truco.campaign.infrastructure.events.CampaignDomainEventDispatcher;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class CampaignConfiguration {

  private final CampaignProgressRepository campaignProgressRepository;
  private final CampaignMatchRegistry campaignMatchRegistry;
  private final UserQueryRepository userQueryRepository;
  private final UseCasePipeline transactionalPipeline;
  private final UseCasePipeline retryTransactionalPipeline;

  public CampaignConfiguration(final CampaignProgressRepository campaignProgressRepository,
      final CampaignMatchRegistry campaignMatchRegistry,
      final UserQueryRepository userQueryRepository,
      @Qualifier("transactionalPipeline") final UseCasePipeline transactionalPipeline,
      @Qualifier("retryTransactionalPipeline") final UseCasePipeline retryTransactionalPipeline) {

    this.campaignProgressRepository = campaignProgressRepository;
    this.campaignMatchRegistry = campaignMatchRegistry;
    this.userQueryRepository = userQueryRepository;
    this.transactionalPipeline = transactionalPipeline;
    this.retryTransactionalPipeline = retryTransactionalPipeline;
  }

  @Bean
  CampaignBotCatalog campaignBotCatalog() {

    return new CampaignBotCatalog();
  }

  @Bean
  CampaignBotCatalogInitializer campaignBotCatalogInitializer(final BotRegistry botRegistry) {

    return new CampaignBotCatalogInitializer(botRegistry, campaignBotCatalog());
  }

  @Bean
  CampaignEventNotifier campaignEventNotifier(final List<CampaignDomainEventHandler<?>> handlers) {

    return new CampaignDomainEventDispatcher(handlers);
  }

  @Bean
  CampaignChallengeResolutionService campaignChallengeResolutionService(
      @Lazy final CampaignEventNotifier campaignEventNotifier) {

    return new CampaignChallengeResolutionService(this.campaignProgressRepository,
        campaignBotCatalog(), this.campaignMatchRegistry, campaignEventNotifier);
  }

  @Bean
  CampaignMatchDomainEventHandler campaignMatchDomainEventHandler(
      final CampaignChallengeResolutionService campaignChallengeResolutionService) {

    return new CampaignMatchDomainEventHandler(campaignChallengeResolutionService);
  }

  @Bean
  CampaignRematchVeto campaignRematchVeto() {

    return new CampaignRematchVeto(this.campaignMatchRegistry);
  }

  @Bean
  CampaignHiddenBotIdsProvider campaignHiddenBotIdsProvider() {

    return new CampaignHiddenBotIdsProvider(campaignBotCatalog());
  }

  @Bean
  CampaignRevealedBotIdsProvider campaignRevealedBotIdsProvider() {

    return new CampaignRevealedBotIdsProvider(this.campaignProgressRepository);
  }

  @Bean
  CampaignUserGuard campaignUserGuard() {

    return new CampaignUserGuard(this.userQueryRepository);
  }

  @Bean
  StartCampaignChallengeUseCase startCampaignChallengeCommandHandler(
      @Lazy final CreateBotMatchUseCase createBotMatchUseCase,
      @Lazy final CampaignEventNotifier campaignEventNotifier) {

    final var handler = new StartCampaignChallengeCommandHandler(this.campaignProgressRepository,
        campaignBotCatalog(), this.campaignMatchRegistry, campaignEventNotifier,
        createBotMatchUseCase, campaignUserGuard());
    return this.retryTransactionalPipeline.wrap(handler)::handle;
  }

  @Bean
  GetCampaignUseCase getCampaignQueryHandler() {

    final var handler = new GetCampaignQueryHandler(this.campaignProgressRepository,
        campaignBotCatalog());
    return this.transactionalPipeline.wrap(handler)::handle;
  }

}
