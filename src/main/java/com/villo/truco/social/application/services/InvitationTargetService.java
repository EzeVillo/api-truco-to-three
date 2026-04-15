package com.villo.truco.social.application.services;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.exceptions.InvitableResourceNotFoundException;
import com.villo.truco.social.application.exceptions.ResourceInvitationTargetUnavailableException;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.util.Objects;

public final class InvitationTargetService {

  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;

  public InvitationTargetService(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
  }

  public void ensureInvitableForSending(final PlayerId senderId,
      final ResourceInvitationTargetType targetType, final String targetId) {

    switch (targetType) {
      case MATCH -> this.ensureInvitableMatch(senderId, targetId);
      case LEAGUE -> this.ensureInvitableLeague(senderId, targetId);
      case CUP -> this.ensureInvitableCup(senderId, targetId);
      default -> throw new IllegalStateException("Unexpected target type: " + targetType);
    }
  }

  private void ensureInvitableMatch(final PlayerId senderId, final String targetId) {

    final var match = this.matchQueryRepository.findById(MatchId.of(targetId)).orElseThrow(
        () -> new InvitableResourceNotFoundException(ResourceInvitationTargetType.MATCH, targetId));
    if (!match.getPlayerOne().equals(senderId) || !this.isJoinableMatch(targetId)) {
      throw new ResourceInvitationTargetUnavailableException();
    }
  }

  private void ensureInvitableLeague(final PlayerId senderId, final String targetId) {

    final var league = this.leagueQueryRepository.findById(LeagueId.of(targetId)).orElseThrow(
        () -> new InvitableResourceNotFoundException(ResourceInvitationTargetType.LEAGUE,
            targetId));
    if (!league.getParticipants().contains(senderId) || !this.isJoinableLeague(targetId)) {
      throw new ResourceInvitationTargetUnavailableException();
    }
  }

  private void ensureInvitableCup(final PlayerId senderId, final String targetId) {

    final var cup = this.cupQueryRepository.findById(CupId.of(targetId)).orElseThrow(
        () -> new InvitableResourceNotFoundException(ResourceInvitationTargetType.CUP, targetId));
    if (!cup.getParticipants().contains(senderId) || !this.isJoinableCup(targetId)) {
      throw new ResourceInvitationTargetUnavailableException();
    }
  }

  private boolean isJoinableMatch(final String targetId) {

    return this.matchQueryRepository.findById(MatchId.of(targetId))
        .filter(match -> match.getStatus() == MatchStatus.WAITING_FOR_PLAYERS)
        .filter(match -> match.getPlayerTwo() == null).isPresent();
  }

  private boolean isJoinableLeague(final String targetId) {

    return this.leagueQueryRepository.findById(LeagueId.of(targetId))
        .filter(league -> league.getStatus() == LeagueStatus.WAITING_FOR_PLAYERS).isPresent();
  }

  private boolean isJoinableCup(final String targetId) {

    return this.cupQueryRepository.findById(CupId.of(targetId))
        .filter(cup -> cup.getStatus() == CupStatus.WAITING_FOR_PLAYERS).isPresent();
  }

}
