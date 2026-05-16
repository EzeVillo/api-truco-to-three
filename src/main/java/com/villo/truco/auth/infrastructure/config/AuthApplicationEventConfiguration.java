package com.villo.truco.auth.infrastructure.config;

import com.villo.truco.auth.domain.ports.AuthEventNotifier;
import com.villo.truco.auth.infrastructure.events.CompositeAuthEventNotifier;
import com.villo.truco.profile.application.eventhandlers.ProfileUserRegisteredEventHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthApplicationEventConfiguration {

  private final ProfileUserRegisteredEventHandler profileUserRegisteredEventHandler;

  public AuthApplicationEventConfiguration(
      final ProfileUserRegisteredEventHandler profileUserRegisteredEventHandler) {

    this.profileUserRegisteredEventHandler = profileUserRegisteredEventHandler;
  }

  @Bean
  AuthEventNotifier authEventNotifier() {

    return new CompositeAuthEventNotifier(List.of(this.profileUserRegisteredEventHandler));
  }

}
