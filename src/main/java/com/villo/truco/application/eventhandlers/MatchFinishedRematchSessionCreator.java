package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.usecases.commands.RematchEligibilityPolicy;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import com.villo.truco.domain.ports.RematchSessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MatchFinishedRematchSessionCreator implements
    MatchDomainEventHandler<MatchFinishedEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      MatchFinishedRematchSessionCreator.class);

  private final RematchSessionRepository rematchSessionRepository;
  private final RematchSessionEventNotifier rematchSessionEventNotifier;
  private final RematchEligibilityPolicy eligibilityPolicy;
  private final BotRegistry botRegistry;
  private final MatchQueryRepository matchQueryRepository;
  private final Duration rematchDuration;
  private final Clock clock;

  public MatchFinishedRematchSessionCreator(final RematchSessionRepository rematchSessionRepository,
      final RematchSessionEventNotifier rematchSessionEventNotifier,
      final RematchEligibilityPolicy eligibilityPolicy, final BotRegistry botRegistry,
      final MatchQueryRepository matchQueryRepository, final Duration rematchDuration,
      final Clock clock) {

    this.rematchSessionRepository = Objects.requireNonNull(rematchSessionRepository);
    this.rematchSessionEventNotifier = Objects.requireNonNull(rematchSessionEventNotifier);
    this.eligibilityPolicy = Objects.requireNonNull(eligibilityPolicy);
    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.rematchDuration = Objects.requireNonNull(rematchDuration);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Class<MatchFinishedEvent> eventType() {

    return MatchFinishedEvent.class;
  }

  @Override
  public void handle(final MatchFinishedEvent finished) {

    final var matchId = finished.getMatchId();

    if (!eligibilityPolicy.isCasualMatch(matchId)) {
      LOGGER.debug("Match {} is not casual, skipping rematch session creation", matchId);
      return;
    }

    final var match = matchQueryRepository.findById(matchId).orElse(null);
    if (match == null) {
      LOGGER.warn("Match {} not found when creating rematch session", matchId);
      return;
    }

    final var gamesToWin = (match.getGamesToPlay() + 1) / 2;
    final var playerOneId = finished.getPlayerOne();
    final var playerTwoId = finished.getPlayerTwo();

    if (playerTwoId == null) {
      LOGGER.debug("Match {} has no playerTwo, skipping rematch session creation", matchId);
      return;
    }

    final var playerOneIsBot = botRegistry.isBot(playerOneId);
    final var playerTwoIsBot = botRegistry.isBot(playerTwoId);

    final var session = RematchSession.open(matchId, playerOneId, playerTwoId, gamesToWin,
        playerOneIsBot, playerTwoIsBot, clock.instant(), rematchDuration);

    rematchSessionRepository.save(session);
    rematchSessionEventNotifier.publishDomainEvents(session.getRematchDomainEvents());
    session.clearDomainEvents();

    LOGGER.info("Rematch session created: sessionId={}, matchId={}, playerTwoIsBot={}",
        session.getId(), matchId, playerTwoIsBot);
  }

}
