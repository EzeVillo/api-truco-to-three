package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.ActiveCupRefDTO;
import com.villo.truco.application.dto.ActiveLeagueRefDTO;
import com.villo.truco.application.dto.ActiveMatchRefDTO;
import com.villo.truco.application.dto.ActiveRematchRefDTO;
import com.villo.truco.application.dto.UserPresenceDTO;
import com.villo.truco.application.ports.in.GetUserPresenceUseCase;
import com.villo.truco.application.queries.GetUserPresenceQuery;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class GetUserPresenceQueryHandler implements GetUserPresenceUseCase {

  private final MatchQueryRepository matchQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final RematchSessionRepository rematchSessionRepository;

  public GetUserPresenceQueryHandler(final MatchQueryRepository matchQueryRepository,
      final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository,
      final RematchSessionRepository rematchSessionRepository) {

    this.matchQueryRepository = Objects.requireNonNull(matchQueryRepository);
    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.rematchSessionRepository = Objects.requireNonNull(rematchSessionRepository);
  }

  @Override
  public UserPresenceDTO handle(final GetUserPresenceQuery query) {

    final var player = query.requester();

    final var matchRef = resolveMatch(player);
    final var currentMatchId = matchRef != null ? matchRef.id() : null;
    final var leagueRef = resolveLeague(player, currentMatchId);
    final var cupRef = resolveCup(player, currentMatchId);
    final var rematchRef = resolveRematch(player);

    return UserPresenceDTO.of(matchRef, leagueRef, cupRef, rematchRef);
  }

  private ActiveMatchRefDTO resolveMatch(final PlayerId player) {

    return this.matchQueryRepository.findUnfinishedByPlayer(player).map(
            match -> new ActiveMatchRefDTO(match.getId().value().toString(), match.getStatus().name()))
        .orElse(null);
  }

  private ActiveLeagueRefDTO resolveLeague(final PlayerId player, final String currentMatchId) {

    final var league = this.leagueQueryRepository.findInProgressByPlayer(player)
        .or(() -> this.leagueQueryRepository.findWaitingByPlayer(player)).orElse(null);

    if (league == null) {
      return null;
    }

    final var tournamentMatchId =
        league.getStatus() == LeagueStatus.IN_PROGRESS ? currentMatchId : null;
    return new ActiveLeagueRefDTO(league.getId().value().toString(), league.getStatus().name(),
        tournamentMatchId);
  }

  private ActiveCupRefDTO resolveCup(final PlayerId player, final String currentMatchId) {

    final var cup = this.cupQueryRepository.findInProgressByPlayer(player)
        .or(() -> this.cupQueryRepository.findWaitingByPlayer(player)).orElse(null);

    if (cup == null) {
      return null;
    }

    final var tournamentMatchId = cup.getStatus() == CupStatus.IN_PROGRESS ? currentMatchId : null;
    return new ActiveCupRefDTO(cup.getId().value().toString(), cup.getStatus().name(),
        tournamentMatchId);
  }

  private ActiveRematchRefDTO resolveRematch(final PlayerId player) {

    return this.rematchSessionRepository.findOpenByPlayer(player).map(
        session -> new ActiveRematchRefDTO(session.getId().value().toString(),
            session.getOriginMatchId().value().toString())).orElse(null);
  }

}
