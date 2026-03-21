package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.villo.truco.application.ports.in.AdvanceCupUseCase;
import com.villo.truco.application.ports.in.ForfeitCupUseCase;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class EventNotifierConfigurationTest {

  @Test
  void buildsNotifierBeans() {

    final var configuration = new EventNotifierConfiguration(mock(SimpMessagingTemplate.class),
        mock(LeagueQueryRepository.class), mock(LeagueRepository.class),
        mock(CupQueryRepository.class), mock(AdvanceCupUseCase.class),
      mock(ForfeitCupUseCase.class), mock(EventNotifierHealthRegistry.class),
      mock(MeterRegistry.class));

    assertThat(configuration.stompMatchEventNotifier()).isNotNull();
    assertThat(configuration.matchEventNotifier()).isNotNull();
  }

}
