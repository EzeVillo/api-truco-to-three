package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.out.RematchSessionDomainEventHandler;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.rematch.events.RematchSessionConfirmedEvent;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import com.villo.truco.domain.ports.RematchSessionRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RematchSessionConfirmedMatchCreator implements
    RematchSessionDomainEventHandler<RematchSessionConfirmedEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      RematchSessionConfirmedMatchCreator.class);

  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final RematchSessionRepository rematchSessionRepository;
  private final RematchSessionEventNotifier rematchSessionEventNotifier;
  private final BotRegistry botRegistry;

  public RematchSessionConfirmedMatchCreator(final MatchRepository matchRepository,
      final MatchEventNotifier matchEventNotifier,
      final RematchSessionRepository rematchSessionRepository,
      final RematchSessionEventNotifier rematchSessionEventNotifier,
      final BotRegistry botRegistry) {

    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.rematchSessionRepository = Objects.requireNonNull(rematchSessionRepository);
    this.rematchSessionEventNotifier = Objects.requireNonNull(rematchSessionEventNotifier);
    this.botRegistry = Objects.requireNonNull(botRegistry);
  }

  @Override
  public Class<RematchSessionConfirmedEvent> eventType() {

    return RematchSessionConfirmedEvent.class;
  }

  @Override
  public void handle(final RematchSessionConfirmedEvent confirmed) {

    final var session = rematchSessionRepository.findById(confirmed.getRematchSessionId())
        .orElse(null);
    if (session == null) {
      LOGGER.warn("Rematch session {} not found when attaching result match",
          confirmed.getRematchSessionId());
      return;
    }
    if (session.getResultMatchId() != null) {
      LOGGER.info("Rematch session {} already has resultMatchId={}, skipping",
          confirmed.getRematchSessionId(), session.getResultMatchId());
      return;
    }

    final var forfeitsOnInactivity =
        !this.botRegistry.isBot(confirmed.getNewPlayerOneId()) && !this.botRegistry.isBot(
            confirmed.getNewPlayerTwoId());
    final var rules = new MatchRules(confirmed.getGamesToWin(), forfeitsOnInactivity);
    final var match = Match.createReady(confirmed.getNewMatchId(), confirmed.getNewPlayerOneId(),
        confirmed.getNewPlayerTwoId(), rules);

    match.startMatch(confirmed.getNewPlayerOneId());
    match.startMatch(confirmed.getNewPlayerTwoId());

    session.attachResultMatch(match.getId());

    matchRepository.save(match);
    rematchSessionRepository.save(session);

    rematchSessionEventNotifier.publishDomainEvents(session.getRematchDomainEvents());
    session.clearDomainEvents();
    matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();

    LOGGER.info("Rematch match auto-started: newMatchId={}, sessionId={}", match.getId(),
        confirmed.getRematchSessionId());
  }

}
