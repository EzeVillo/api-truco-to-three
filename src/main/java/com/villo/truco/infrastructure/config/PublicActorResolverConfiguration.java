package com.villo.truco.infrastructure.config;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.infrastructure.identity.DefaultPublicActorResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicActorResolverConfiguration {

  @Bean
  PublicActorResolver publicActorResolver(final UserRepository userRepository,
      final BotRegistry botRegistry) {

    return new DefaultPublicActorResolver(userRepository, botRegistry);
  }

}
