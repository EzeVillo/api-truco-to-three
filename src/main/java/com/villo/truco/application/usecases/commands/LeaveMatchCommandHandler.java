package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.commands.LeaveMatchCommand;
import com.villo.truco.application.exceptions.MatchBelongsToCompetitionException;
import com.villo.truco.application.ports.in.LeaveMatchUseCase;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
import java.util.Objects;

public final class LeaveMatchCommandHandler implements LeaveMatchUseCase {

  private final MatchResolver matchResolver;
  private final MatchRepository matchRepository;
  private final MatchEventNotifier matchEventNotifier;
  private final CupQueryRepository cupQueryRepository;
  private final LeagueQueryRepository leagueQueryRepository;

  public LeaveMatchCommandHandler(final MatchResolver matchResolver,
      final MatchRepository matchRepository, final MatchEventNotifier matchEventNotifier,
      final CupQueryRepository cupQueryRepository,
      final LeagueQueryRepository leagueQueryRepository) {

    this.matchResolver = Objects.requireNonNull(matchResolver);
    this.matchRepository = Objects.requireNonNull(matchRepository);
    this.matchEventNotifier = Objects.requireNonNull(matchEventNotifier);
    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
  }

  @Override
  public Void handle(final LeaveMatchCommand command) {

    if (this.cupQueryRepository.findByMatchId(command.matchId()).isPresent()
        || this.leagueQueryRepository.findByMatchId(command.matchId()).isPresent()) {
      throw new MatchBelongsToCompetitionException();
    }

    final var match = this.matchResolver.resolve(command.matchId());

    match.leave(command.playerId());

    this.matchRepository.save(match);
    this.matchEventNotifier.publishDomainEvents(match.getMatchDomainEvents());
    match.clearDomainEvents();

    return null;
  }

}
