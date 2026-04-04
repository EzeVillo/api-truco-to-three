package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.assemblers.PublicLeagueLobbyDTOAssembler;
import com.villo.truco.application.events.PublicLeagueLobbyNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerJoinedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerLeftEvent;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.events.PublicLeagueLobbyOpenedEvent;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import java.util.Map;
import java.util.Objects;

public final class PublicLeagueLobbyEventTranslator implements
    LeagueDomainEventHandler<LeagueDomainEvent> {

  private static final String UPSERT_EVENT_TYPE = "PUBLIC_LEAGUE_LOBBY_UPSERT";
  private static final String REMOVED_EVENT_TYPE = "PUBLIC_LEAGUE_LOBBY_REMOVED";

  private final LeagueQueryRepository leagueQueryRepository;
  private final PublicLeagueLobbyDTOAssembler assembler;
  private final ApplicationEventPublisher publisher;

  public PublicLeagueLobbyEventTranslator(final LeagueQueryRepository leagueQueryRepository,
      final PublicLeagueLobbyDTOAssembler assembler, final ApplicationEventPublisher publisher) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.assembler = Objects.requireNonNull(assembler);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<LeagueDomainEvent> eventType() {

    return LeagueDomainEvent.class;
  }

  @Override
  public void handle(final LeagueDomainEvent event) {

    if (!(event instanceof PublicLeagueLobbyOpenedEvent || event instanceof LeaguePlayerJoinedEvent
        || event instanceof LeaguePlayerLeftEvent || event instanceof LeagueCancelledEvent
        || event instanceof LeagueStartedEvent)) {
      return;
    }

    final var notification = this.leagueQueryRepository.findById(event.getLeagueId())
        .filter(League::isPublicLobbyOpen).map(
            league -> new PublicLeagueLobbyNotification(UPSERT_EVENT_TYPE, event.getTimestamp(),
                Map.of("lobby", this.assembler.assemble(league)))).orElseGet(
            () -> new PublicLeagueLobbyNotification(REMOVED_EVENT_TYPE, event.getTimestamp(),
                Map.of("id", event.getLeagueId().value().toString())));
    this.publisher.publish(notification);
  }

}
