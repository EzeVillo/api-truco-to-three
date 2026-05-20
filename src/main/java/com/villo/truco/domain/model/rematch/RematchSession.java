package com.villo.truco.domain.model.rematch;

import com.villo.truco.domain.model.rematch.events.RematchPlayerWantsRematchEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionClosedByLeaveEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionConfirmedEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionExpiredEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionOpenedEvent;
import com.villo.truco.domain.model.rematch.exceptions.BotCannotLeaveRematchSessionException;
import com.villo.truco.domain.model.rematch.exceptions.NotParticipantOfRematchSessionException;
import com.villo.truco.domain.model.rematch.exceptions.RematchPlayerAlreadyLeftException;
import com.villo.truco.domain.model.rematch.exceptions.RematchSessionExpiredException;
import com.villo.truco.domain.model.rematch.exceptions.RematchSessionNotOpenException;
import com.villo.truco.domain.model.rematch.valueobjects.RematchPlayerChoice;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionStatus;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class RematchSession extends AggregateBase<RematchSessionId> {

  private final MatchId originMatchId;
  private final PlayerId playerOneId;
  private final PlayerId playerTwoId;
  private final boolean playerOneIsBot;
  private final boolean playerTwoIsBot;
  private final int gamesToWin;
  private final Instant expiresAt;
  private RematchPlayerChoice playerOneChoice;
  private RematchPlayerChoice playerTwoChoice;
  private RematchSessionStatus status;
  private MatchId resultMatchId;

  private RematchSession(final RematchSessionId id, final MatchId originMatchId,
      final PlayerId playerOneId, final PlayerId playerTwoId, final boolean playerOneIsBot,
      final boolean playerTwoIsBot, final int gamesToWin, final Instant expiresAt,
      final RematchPlayerChoice playerOneChoice, final RematchPlayerChoice playerTwoChoice,
      final RematchSessionStatus status, final MatchId resultMatchId) {

    super(id);
    this.originMatchId = Objects.requireNonNull(originMatchId);
    this.playerOneId = Objects.requireNonNull(playerOneId);
    this.playerTwoId = Objects.requireNonNull(playerTwoId);
    this.playerOneIsBot = playerOneIsBot;
    this.playerTwoIsBot = playerTwoIsBot;
    this.gamesToWin = gamesToWin;
    this.expiresAt = Objects.requireNonNull(expiresAt);
    this.playerOneChoice = Objects.requireNonNull(playerOneChoice);
    this.playerTwoChoice = Objects.requireNonNull(playerTwoChoice);
    this.status = Objects.requireNonNull(status);
    this.resultMatchId = resultMatchId;
  }

  public static RematchSession open(final MatchId originMatchId, final PlayerId playerOneId,
      final PlayerId playerTwoId, final int gamesToWin, final boolean playerOneIsBot,
      final boolean playerTwoIsBot, final Instant now, final Duration ttl) {

    final var id = RematchSessionId.generate();
    final var expiresAt = now.plus(ttl);
    final var p1Choice =
        playerOneIsBot ? RematchPlayerChoice.WANTS_REMATCH : RematchPlayerChoice.UNDECIDED;
    final var p2Choice =
        playerTwoIsBot ? RematchPlayerChoice.WANTS_REMATCH : RematchPlayerChoice.UNDECIDED;
    final var session = new RematchSession(id, originMatchId, playerOneId, playerTwoId,
        playerOneIsBot, playerTwoIsBot, gamesToWin, expiresAt, p1Choice, p2Choice,
        RematchSessionStatus.OPEN, null);
    session.addDomainEvent(
        new RematchSessionOpenedEvent(id, originMatchId, playerOneId, playerTwoId, expiresAt,
            playerOneIsBot, playerTwoIsBot));
    return session;
  }

  public static RematchSession reconstruct(final RematchSessionId id, final MatchId originMatchId,
      final PlayerId playerOneId, final PlayerId playerTwoId, final boolean playerOneIsBot,
      final boolean playerTwoIsBot, final int gamesToWin, final Instant expiresAt,
      final RematchPlayerChoice playerOneChoice, final RematchPlayerChoice playerTwoChoice,
      final RematchSessionStatus status, final MatchId resultMatchId) {

    return new RematchSession(id, originMatchId, playerOneId, playerTwoId, playerOneIsBot,
        playerTwoIsBot, gamesToWin, expiresAt, playerOneChoice, playerTwoChoice, status,
        resultMatchId);
  }

  public void chooseRematch(final PlayerId actor, final Instant now, final MatchId newMatchId) {

    this.ensureOpen();
    this.ensureNotExpired(now);

    if (!isParticipant(actor)) {
      throw new NotParticipantOfRematchSessionException();
    }
    if (isPlayerOne(actor) && playerOneIsBot) {
      return;
    }
    if (!isPlayerOne(actor) && playerTwoIsBot) {
      return;
    }
    final var current = currentChoiceOf(actor);
    if (current == RematchPlayerChoice.LEFT) {
      throw new RematchPlayerAlreadyLeftException();
    }
    if (current == RematchPlayerChoice.WANTS_REMATCH) {
      return;
    }
    setChoiceOf(actor, RematchPlayerChoice.WANTS_REMATCH);
    addDomainEvent(
        new RematchPlayerWantsRematchEvent(id, originMatchId, actor, otherPlayer(actor)));

    if (playerOneChoice == RematchPlayerChoice.WANTS_REMATCH
        && playerTwoChoice == RematchPlayerChoice.WANTS_REMATCH) {
      status = RematchSessionStatus.CONFIRMED;
      addDomainEvent(
          new RematchSessionConfirmedEvent(id, originMatchId, newMatchId, playerTwoId, playerOneId,
              gamesToWin));
    }
  }

  public void leave(final PlayerId actor) {

    this.ensureOpen();

    if (!isParticipant(actor)) {
      throw new NotParticipantOfRematchSessionException();
    }
    if (isPlayerOne(actor) && playerOneIsBot) {
      throw new BotCannotLeaveRematchSessionException();
    }
    if (!isPlayerOne(actor) && playerTwoIsBot) {
      throw new BotCannotLeaveRematchSessionException();
    }
    setChoiceOf(actor, RematchPlayerChoice.LEFT);
    status = RematchSessionStatus.CLOSED_BY_LEAVE;
    addDomainEvent(
        new RematchSessionClosedByLeaveEvent(id, originMatchId, actor, otherPlayer(actor)));
  }

  public void expireIfNeeded(final Instant now) {

    if (status != RematchSessionStatus.OPEN) {
      return;
    }
    if (now.isBefore(expiresAt)) {
      return;
    }
    status = RematchSessionStatus.EXPIRED;
    addDomainEvent(new RematchSessionExpiredEvent(id, originMatchId, playerOneId, playerTwoId));
  }

  public void attachResultMatch(final MatchId resultMatchId) {

    if (status != RematchSessionStatus.CONFIRMED) {
      throw new RematchSessionNotOpenException();
    }
    if (this.resultMatchId != null) {
      return;
    }
    this.resultMatchId = resultMatchId;
  }

  private boolean isPlayerOne(final PlayerId actor) {

    return playerOneId.equals(actor);
  }

  private RematchPlayerChoice currentChoiceOf(final PlayerId actor) {

    return isPlayerOne(actor) ? playerOneChoice : playerTwoChoice;
  }

  private void setChoiceOf(final PlayerId actor, final RematchPlayerChoice choice) {

    if (isPlayerOne(actor)) {
      playerOneChoice = choice;
    } else {
      playerTwoChoice = choice;
    }
  }

  private PlayerId otherPlayer(final PlayerId actor) {

    return isPlayerOne(actor) ? playerTwoId : playerOneId;
  }

  private void ensureOpen() {

    if (status != RematchSessionStatus.OPEN) {
      throw new RematchSessionNotOpenException();
    }
  }

  private void ensureNotExpired(final Instant now) {

    if (!now.isBefore(expiresAt)) {
      expireIfNeeded(now);
      throw new RematchSessionExpiredException();
    }
  }

  public MatchId getOriginMatchId() {

    return originMatchId;
  }

  public PlayerId getPlayerOneId() {

    return playerOneId;
  }

  public PlayerId getPlayerTwoId() {

    return playerTwoId;
  }

  public boolean isPlayerOneIsBot() {

    return playerOneIsBot;
  }

  public boolean isPlayerTwoIsBot() {

    return playerTwoIsBot;
  }

  public int getGamesToWin() {

    return gamesToWin;
  }

  public Instant getExpiresAt() {

    return expiresAt;
  }

  public RematchPlayerChoice getPlayerOneChoice() {

    return playerOneChoice;
  }

  public RematchPlayerChoice getPlayerTwoChoice() {

    return playerTwoChoice;
  }

  public RematchSessionStatus getStatus() {

    return status;
  }

  public MatchId getResultMatchId() {

    return resultMatchId;
  }

  public boolean isParticipant(final PlayerId playerId) {

    return playerOneId.equals(playerId) || playerTwoId.equals(playerId);
  }

  public List<RematchSessionDomainEvent> getRematchDomainEvents() {

    return getDomainEvents().stream().map(RematchSessionDomainEvent.class::cast).toList();
  }

}
