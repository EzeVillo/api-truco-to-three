package com.villo.truco.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.ports.out.LeagueEventContext;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompLeagueEventNotifier")
class StompLeagueEventNotifierTest {

    @Test
    @DisplayName("envía el evento a todos los participantes")
    void broadcastsToAllParticipants() {

        final var messaging = mock(SimpMessagingTemplate.class);
        final var notifier = new StompLeagueEventNotifier(messaging,
            mock(EventNotifierHealthRegistry.class));
        final var leagueId = LeagueId.generate();
        final var participants = List.of(PlayerId.generate(), PlayerId.generate(),
            PlayerId.generate());
        final var context = new LeagueEventContext(leagueId, participants);

        notifier.handle(new LeagueStartedEvent(leagueId), context);

        verify(messaging, times(3)).convertAndSendToUser(any(), eq("/queue/events"), any());
    }

}
