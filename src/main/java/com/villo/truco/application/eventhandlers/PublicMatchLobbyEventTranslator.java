package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.assemblers.PublicMatchLobbyDTOAssembler;
import com.villo.truco.application.events.PublicMatchLobbyNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.PublicMatchLobbyOpenedEvent;
import com.villo.truco.domain.ports.MatchQueryRepository;
import java.util.Map;
import java.util.Objects;

public final class PublicMatchLobbyEventTranslator implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private static final String UPSERT_EVENT_TYPE = "PUBLIC_MATCH_LOBBY_UPSERT";
  private static final String REMOVED_EVENT_TYPE = "PUBLIC_MATCH_LOBBY_REMOVED";

  private final MatchQueryRepository matchQueryRepository;
  private final PublicMatchLobbyDTOAssembler assembler;
  private final ApplicationEventPublisher publisher;

  public PublicMatchLobbyEventTranslator(final MatchQueryRepository matchQueryRepository,
      final PublicMatchLobbyDTOAssembler assembler, final ApplicationEventPublisher publisher) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.assembler = Objects.requireNonNull(assembler);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    if (!(event instanceof PublicMatchLobbyOpenedEvent || event instanceof PlayerJoinedEvent
        || event instanceof MatchCancelledEvent)) {
      return;
    }

    final var notification = this.matchQueryRepository.findById(event.getMatchId())
        .filter(Match::isPublicLobbyOpen).map(
            match -> new PublicMatchLobbyNotification(UPSERT_EVENT_TYPE, event.getTimestamp(),
                Map.of("lobby", this.assembler.assemble(match)))).orElseGet(
            () -> new PublicMatchLobbyNotification(REMOVED_EVENT_TYPE, event.getTimestamp(),
                Map.of("id", event.getMatchId().value().toString())));
    this.publisher.publish(notification);
  }

}
