package com.villo.truco.campaign.domain.model;

import com.villo.truco.campaign.domain.model.events.AllCampaignBotsUnlockedForCasualEvent;
import com.villo.truco.campaign.domain.model.events.CampaignAllRivalsDefeatedEvent;
import com.villo.truco.campaign.domain.model.events.CampaignBotUnlockedForCasualEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeLostEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeStartedEvent;
import com.villo.truco.campaign.domain.model.events.CampaignChallengeWonEvent;
import com.villo.truco.campaign.domain.model.events.CampaignDomainEvent;
import com.villo.truco.campaign.domain.model.events.CampaignTopOneReachedEvent;
import com.villo.truco.campaign.domain.model.exceptions.BotNotImmediatelyAboveException;
import com.villo.truco.campaign.domain.model.exceptions.CampaignChallengeAlreadyActiveException;
import com.villo.truco.campaign.domain.model.exceptions.NoActiveCampaignChallengeException;
import com.villo.truco.campaign.domain.model.exceptions.NoNextRivalAvailableException;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignChallenge;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignPoints;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignRivalRecord;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class CampaignProgress extends AggregateBase<PlayerId> {

  static final int CASUAL_UNLOCK_THRESHOLD = 3;

  private final Map<PlayerId, CampaignRivalRecord> rivalRecords = new LinkedHashMap<>();
  private final Set<PlayerId> unlockedCasualBots = new LinkedHashSet<>();
  private CampaignPoints points = CampaignPoints.ZERO;
  private CampaignChallenge activeChallenge;
  private boolean topOneReached;
  private boolean allRivalsDefeated;
  private boolean allCasualBotsUnlocked;

  private CampaignProgress(final PlayerId playerId) {

    super(Objects.requireNonNull(playerId));
  }

  public static CampaignProgress create(final PlayerId playerId) {

    return new CampaignProgress(playerId);
  }

  static CampaignProgress reconstruct(final PlayerId playerId, final CampaignPoints points,
      final CampaignChallenge activeChallenge,
      final Map<PlayerId, CampaignRivalRecord> rivalRecords, final boolean topOneReached,
      final boolean allRivalsDefeated, final Set<PlayerId> unlockedCasualBots,
      final boolean allCasualBotsUnlocked) {

    final var progress = new CampaignProgress(playerId);
    progress.points = Objects.requireNonNull(points);
    progress.activeChallenge = activeChallenge;
    progress.rivalRecords.putAll(rivalRecords);
    progress.topOneReached = topOneReached;
    progress.allRivalsDefeated = allRivalsDefeated;
    progress.unlockedCasualBots.addAll(unlockedCasualBots);
    progress.allCasualBotsUnlocked = allCasualBotsUnlocked;
    return progress;
  }

  public boolean canChallenge(final CampaignBot rival, final CampaignLadder ladder) {

    Objects.requireNonNull(rival, "rival cannot be null");
    Objects.requireNonNull(ladder, "ladder cannot be null");

    if (this.activeChallenge != null) {
      return false;
    }

    if (this.topOneReached) {
      return true;
    }

    return ladder.nextRival(this.points)
        .map(nextRival -> nextRival.playerId().equals(rival.playerId())).orElse(false);
  }

  public void ensureCanChallenge(final CampaignBot rival, final CampaignLadder ladder) {

    Objects.requireNonNull(rival, "rival cannot be null");
    Objects.requireNonNull(ladder, "ladder cannot be null");

    if (this.activeChallenge != null) {
      throw new CampaignChallengeAlreadyActiveException(this.id, this.activeChallenge.matchId());
    }

    if (this.topOneReached) {
      return;
    }

    final var nextRival = ladder.nextRival(this.points)
        .orElseThrow(NoNextRivalAvailableException::new);

    if (!nextRival.playerId().equals(rival.playerId())) {
      throw new BotNotImmediatelyAboveException(rival.playerId());
    }
  }

  public void startChallenge(final CampaignBot rival, final MatchId matchId,
      final CampaignLadder ladder) {

    Objects.requireNonNull(matchId, "matchId cannot be null");

    this.ensureCanChallenge(rival, ladder);
    this.activeChallenge = new CampaignChallenge(matchId, rival.playerId());
    this.addDomainEvent(new CampaignChallengeStartedEvent(this.id, rival.playerId(), matchId));
  }

  public boolean isActiveChallenge(final MatchId matchId) {

    return this.activeChallenge != null && this.activeChallenge.matchId().equals(matchId);
  }

  public void resolveChallengeWon(final MatchId matchId, final int gamesWonPlayer,
      final int gamesWonRival, final CampaignLadder ladder) {

    Objects.requireNonNull(ladder, "ladder cannot be null");
    final var challenge = this.requireActiveChallenge(matchId);

    final var previousPosition = ladder.positionFor(this.points);
    final var pointsAwarded = CampaignScoringPolicy.pointsForVictory(gamesWonPlayer, gamesWonRival);
    this.points = this.points.plus(pointsAwarded);
    this.recordResult(challenge.rivalId(), CampaignRivalRecord::withWin);
    this.activeChallenge = null;

    final var newPosition = ladder.positionFor(this.points);
    this.addDomainEvent(
        new CampaignChallengeWonEvent(this.id, challenge.rivalId(), matchId, pointsAwarded,
            this.points.value(), previousPosition, newPosition));

    final var decidingGameNumber = gamesWonPlayer + gamesWonRival;

    if (newPosition == 1 && !this.topOneReached) {
      this.topOneReached = true;
      this.addDomainEvent(new CampaignTopOneReachedEvent(this.id, matchId, decidingGameNumber));
    }

    if (!this.allRivalsDefeated && this.hasDefeatedAllRivals(ladder)) {
      this.allRivalsDefeated = true;
      this.addDomainEvent(new CampaignAllRivalsDefeatedEvent(this.id, matchId, decidingGameNumber));
    }

    this.evaluateCasualUnlocks(challenge.rivalId(), matchId, decidingGameNumber, ladder);
  }

  private void evaluateCasualUnlocks(final PlayerId rivalId, final MatchId matchId,
      final int decidingGameNumber, final CampaignLadder ladder) {

    final var record = this.rivalRecords.getOrDefault(rivalId, CampaignRivalRecord.EMPTY);
    if (!record.isFavorableBy(CASUAL_UNLOCK_THRESHOLD) || this.unlockedCasualBots.contains(
        rivalId)) {
      return;
    }

    this.unlockedCasualBots.add(rivalId);
    this.addDomainEvent(
        new CampaignBotUnlockedForCasualEvent(this.id, rivalId, matchId, decidingGameNumber));

    if (!this.allCasualBotsUnlocked && this.unlockedCasualBots.size() == ladder.bots().size()) {
      this.allCasualBotsUnlocked = true;
      this.addDomainEvent(
          new AllCampaignBotsUnlockedForCasualEvent(this.id, matchId, decidingGameNumber));
    }
  }

  public void resolveChallengeLost(final MatchId matchId, final CampaignLadder ladder) {

    Objects.requireNonNull(ladder, "ladder cannot be null");
    final var challenge = this.requireActiveChallenge(matchId);

    this.recordResult(challenge.rivalId(), CampaignRivalRecord::withLoss);
    this.activeChallenge = null;

    final var position = ladder.positionFor(this.points);
    this.addDomainEvent(
        new CampaignChallengeLostEvent(this.id, challenge.rivalId(), matchId, this.points.value(),
            position, position));
  }

  private CampaignChallenge requireActiveChallenge(final MatchId matchId) {

    Objects.requireNonNull(matchId, "matchId cannot be null");

    if (!this.isActiveChallenge(matchId)) {
      throw new NoActiveCampaignChallengeException(this.id, matchId);
    }
    return this.activeChallenge;
  }

  private void recordResult(final PlayerId rivalId,
      final UnaryOperator<CampaignRivalRecord> update) {

    this.rivalRecords.merge(rivalId, update.apply(CampaignRivalRecord.EMPTY),
        (existing, ignored) -> update.apply(existing));
  }

  private boolean hasDefeatedAllRivals(final CampaignLadder ladder) {

    return ladder.bots().stream().allMatch(
        bot -> this.rivalRecords.getOrDefault(bot.playerId(), CampaignRivalRecord.EMPTY).hasWin());
  }

  public CampaignPoints getPoints() {

    return this.points;
  }

  public CampaignChallenge getActiveChallenge() {

    return this.activeChallenge;
  }

  public Map<PlayerId, CampaignRivalRecord> getRivalRecords() {

    return Map.copyOf(this.rivalRecords);
  }

  public boolean isTopOneReached() {

    return this.topOneReached;
  }

  public boolean isAllRivalsDefeated() {

    return this.allRivalsDefeated;
  }

  public Set<PlayerId> getUnlockedCasualBots() {

    return Set.copyOf(this.unlockedCasualBots);
  }

  public boolean isAllCasualBotsUnlocked() {

    return this.allCasualBotsUnlocked;
  }

  public List<CampaignDomainEvent> getCampaignDomainEvents() {

    return this.getDomainEvents().stream().map(CampaignDomainEvent.class::cast).toList();
  }

  public CampaignProgressSnapshot snapshot() {

    return new CampaignProgressSnapshot(this.id, this.points, this.activeChallenge,
        new LinkedHashMap<>(this.rivalRecords), this.topOneReached, this.allRivalsDefeated,
        new LinkedHashSet<>(this.unlockedCasualBots), this.allCasualBotsUnlocked);
  }

}
