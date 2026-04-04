package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.assemblers.PublicCupLobbyDTOAssembler;
import com.villo.truco.application.events.PublicCupLobbyNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.events.CupCancelledEvent;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerJoinedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerLeftEvent;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.model.cup.events.PublicCupLobbyOpenedEvent;
import com.villo.truco.domain.ports.CupQueryRepository;
import java.util.Map;
import java.util.Objects;

public final class PublicCupLobbyEventTranslator implements CupDomainEventHandler<CupDomainEvent> {

  private static final String UPSERT_EVENT_TYPE = "PUBLIC_CUP_LOBBY_UPSERT";
  private static final String REMOVED_EVENT_TYPE = "PUBLIC_CUP_LOBBY_REMOVED";

  private final CupQueryRepository cupQueryRepository;
  private final PublicCupLobbyDTOAssembler assembler;
  private final ApplicationEventPublisher publisher;

  public PublicCupLobbyEventTranslator(final CupQueryRepository cupQueryRepository,
      final PublicCupLobbyDTOAssembler assembler, final ApplicationEventPublisher publisher) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.assembler = Objects.requireNonNull(assembler);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public Class<CupDomainEvent> eventType() {

    return CupDomainEvent.class;
  }

  @Override
  public void handle(final CupDomainEvent event) {

    if (!(event instanceof PublicCupLobbyOpenedEvent || event instanceof CupPlayerJoinedEvent
        || event instanceof CupPlayerLeftEvent || event instanceof CupCancelledEvent
        || event instanceof CupStartedEvent)) {
      return;
    }

    final var notification = this.cupQueryRepository.findById(event.getCupId())
        .filter(Cup::isPublicLobbyOpen).map(
            cup -> new PublicCupLobbyNotification(UPSERT_EVENT_TYPE, event.getTimestamp(),
                Map.of("lobby", this.assembler.assemble(cup)))).orElseGet(
            () -> new PublicCupLobbyNotification(REMOVED_EVENT_TYPE, event.getTimestamp(),
                Map.of("id", event.getCupId().value().toString())));
    this.publisher.publish(notification);
  }

}
