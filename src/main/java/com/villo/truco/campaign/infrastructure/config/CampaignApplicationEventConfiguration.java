package com.villo.truco.campaign.infrastructure.config;

import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.campaign.application.eventhandlers.CampaignNotificationEventTranslator;
import com.villo.truco.campaign.infrastructure.websocket.StompCampaignNotificationHandler;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class CampaignApplicationEventConfiguration {

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry eventNotifierHealthRegistry;

  public CampaignApplicationEventConfiguration(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry eventNotifierHealthRegistry) {

    this.messagingTemplate = messagingTemplate;
    this.eventNotifierHealthRegistry = eventNotifierHealthRegistry;
  }

  @Bean
  StompCampaignNotificationHandler stompCampaignNotificationHandler() {

    return new StompCampaignNotificationHandler(this.messagingTemplate,
        this.eventNotifierHealthRegistry);
  }

  @Bean
  CampaignNotificationEventTranslator campaignNotificationEventTranslator(
      @Lazy final ApplicationEventPublisher applicationEventPublisher) {

    return new CampaignNotificationEventTranslator(applicationEventPublisher);
  }

}
