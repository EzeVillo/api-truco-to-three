package com.villo.truco.profile.application.services;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.events.EnvidoCalledEvent;
import com.villo.truco.domain.model.match.events.EnvidoResolvedEvent;
import com.villo.truco.domain.model.match.events.FoldedEvent;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.HandResolvedEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.RoundEndedEvent;
import com.villo.truco.domain.model.match.events.RoundStartedEvent;
import com.villo.truco.domain.model.match.events.ScoreChangedEvent;
import com.villo.truco.domain.model.match.events.TrucoCalledEvent;
import com.villo.truco.domain.model.match.events.TrucoRespondedEvent;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.profile.domain.model.AchievementPolicy;
import com.villo.truco.profile.domain.model.MatchAchievementTracker;
import com.villo.truco.profile.domain.model.PlayerProfile;
import com.villo.truco.profile.domain.ports.MatchAchievementTrackerRepository;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.ProfileEventNotifier;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ProfileAchievementTrackingService {

  private final BotRegistry botRegistry;
  private final UserQueryRepository userQueryRepository;
  private final MatchAchievementTrackerRepository matchAchievementTrackerRepository;
  private final PlayerProfileRepository playerProfileRepository;
  private final AchievementPolicy achievementPolicy;
  private final ProfileEventNotifier profileEventNotifier;

  public ProfileAchievementTrackingService(final BotRegistry botRegistry,
      final UserQueryRepository userQueryRepository,
      final MatchAchievementTrackerRepository matchAchievementTrackerRepository,
      final PlayerProfileRepository playerProfileRepository,
      final AchievementPolicy achievementPolicy, final ProfileEventNotifier profileEventNotifier) {

    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
    this.matchAchievementTrackerRepository = Objects.requireNonNull(matchAchievementTrackerRepository);
    this.playerProfileRepository = Objects.requireNonNull(playerProfileRepository);
    this.achievementPolicy = Objects.requireNonNull(achievementPolicy);
    this.profileEventNotifier = Objects.requireNonNull(profileEventNotifier);
  }

  public void handle(final MatchDomainEvent event) {

    if (event.getPlayerTwo() == null) {
      return;
    }

    final var humanVsHuman = !this.botRegistry.isBot(event.getPlayerOne()) && !this.botRegistry.isBot(
        event.getPlayerTwo());
    if (!humanVsHuman) {
      this.matchAchievementTrackerRepository.deleteByMatchId(event.getMatchId());
      return;
    }

    final var inner = unwrap(event);
    final var tracker = this.resolveTracker(event, inner);
    if (tracker == null) {
      return;
    }

    this.applyInnerEvent(tracker, inner);
    final var decisions = this.achievementPolicy.decideUnlocks(tracker);
    this.matchAchievementTrackerRepository.save(tracker);
    if (decisions.isEmpty()) {
      return;
    }

    final Set<PlayerId> involvedPlayers = decisions.stream().map(d -> d.playerId())
        .collect(java.util.stream.Collectors.toSet());
    final var registeredPlayers = this.userQueryRepository.findUsernamesByIds(involvedPlayers).keySet();
    final var profilesByPlayer = new HashMap<PlayerId, PlayerProfile>();
    final var unlockedAt = Instant.ofEpochMilli(inner.getTimestamp());

    for (final var decision : decisions) {
      if (!registeredPlayers.contains(decision.playerId())) {
        continue;
      }
      final var profile = profilesByPlayer.computeIfAbsent(decision.playerId(),
          playerId -> this.playerProfileRepository.findByPlayerId(playerId)
              .orElseGet(() -> PlayerProfile.create(playerId)));
      profile.unlock(decision.achievementCode(), unlockedAt, event.getMatchId(),
          tracker.getCurrentGameNumber());
    }

    for (final var profile : profilesByPlayer.values()) {
      if (profile.getAchievementUnlockedEvents().isEmpty()) {
        continue;
      }
      this.playerProfileRepository.save(profile);
      this.profileEventNotifier.publishDomainEvents(profile.getAchievementUnlockedEvents());
      profile.clearDomainEvents();
    }
  }

  private MatchAchievementTracker resolveTracker(final MatchDomainEvent event,
      final DomainEventBase inner) {

    return this.matchAchievementTrackerRepository.findByMatchId(event.getMatchId()).orElseGet(() -> {
      if (!(inner instanceof GameStartedEvent)) {
        return null;
      }
      return MatchAchievementTracker.create(event.getMatchId(), event.getPlayerOne(),
          event.getPlayerTwo(), true);
    });
  }

  private DomainEventBase unwrap(final MatchDomainEvent event) {

    if (event instanceof MatchEventEnvelope envelope) {
      return envelope.getInner();
    }
    return event;
  }

  private void applyInnerEvent(final MatchAchievementTracker tracker,
      final DomainEventBase innerEvent) {

    switch (innerEvent) {
      case GameStartedEvent event -> tracker.onGameStarted(event.getGameNumber());
      case RoundStartedEvent event -> tracker.onRoundStarted(event.getRoundNumber(),
          event.getManoSeat());
      case EnvidoCalledEvent event -> tracker.onEnvidoCalled(event.getCall());
      case EnvidoResolvedEvent event -> tracker.onEnvidoResolved(event.getResponse(),
          event.getWinnerSeat(), event.getPointsMano(), event.getPointsPie());
      case TrucoCalledEvent event -> tracker.onTrucoCalled(event.getCall());
      case TrucoRespondedEvent event -> tracker.onTrucoResponded(event.getResponderSeat(),
          event.getResponse(), event.getCall());
      case HandResolvedEvent event -> tracker.onHandResolved(event.getCardPlayerOne(),
          event.getCardPlayerTwo(), event.getWinnerSeat());
      case FoldedEvent event -> tracker.onFolded(event.getSeat());
      case RoundEndedEvent event -> tracker.onRoundEnded(event.getWinnerSeat());
      case ScoreChangedEvent event -> tracker.onScoreChanged(event.getScorePlayerOne(),
          event.getScorePlayerTwo());
      default -> tracker.onOtherEvent();
    }
  }
}
