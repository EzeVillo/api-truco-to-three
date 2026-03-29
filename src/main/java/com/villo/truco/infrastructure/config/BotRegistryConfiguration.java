package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.infrastructure.bot.InMemoryBotRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotRegistryConfiguration {

  @Bean
  BotRegistry botRegistry() {

    return new InMemoryBotRegistry();
  }

}
