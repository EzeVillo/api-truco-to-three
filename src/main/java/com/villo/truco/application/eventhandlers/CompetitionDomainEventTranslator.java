package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.MatchAbandoned;
import com.villo.truco.application.events.MatchCompleted;
import com.villo.truco.application.events.MatchForfeited;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import java.util.Objects;

public final class CompetitionDomainEventTranslator implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final ApplicationEventPublisher applicationEventPublisher;

  public CompetitionDomainEventTranslator(
      final ApplicationEventPublisher applicationEventPublisher) {

    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    if (event instanceof MatchFinishedEvent finished) {
      final var winner = finished.resolvePlayer(finished.getWinnerSeat());
      this.applicationEventPublisher.publish(new MatchCompleted(finished.getMatchId(), winner));
    } else if (event instanceof MatchAbandonedEvent abandoned) {
      final var winner = abandoned.resolvePlayer(abandoned.getWinnerSeat());
      final var abandoner = abandoned.resolvePlayer(abandoned.getAbandonerSeat());
      this.applicationEventPublisher.publish(
          new MatchAbandoned(abandoned.getMatchId(), winner, abandoner));
    } else if (event instanceof MatchForfeitedEvent forfeited) {
      final var winner = forfeited.resolvePlayer(forfeited.getWinnerSeat());
      final var loserSeat =
          forfeited.getWinnerSeat() == PlayerSeat.PLAYER_ONE ? PlayerSeat.PLAYER_TWO
              : PlayerSeat.PLAYER_ONE;
      final var loser = forfeited.resolvePlayer(loserSeat);
      this.applicationEventPublisher.publish(
          new MatchForfeited(forfeited.getMatchId(), winner, loser));
    }
  }

}
