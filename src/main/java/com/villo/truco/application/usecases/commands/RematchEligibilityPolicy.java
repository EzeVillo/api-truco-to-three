package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.ports.RematchVeto;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import java.util.List;
import java.util.Objects;

public final class RematchEligibilityPolicy {

  private final LeagueQueryRepository leagueQueryRepository;
  private final CupQueryRepository cupQueryRepository;
  private final List<RematchVeto> rematchVetoes;

  public RematchEligibilityPolicy(final LeagueQueryRepository leagueQueryRepository,
      final CupQueryRepository cupQueryRepository, final List<RematchVeto> rematchVetoes) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.rematchVetoes = List.copyOf(Objects.requireNonNull(rematchVetoes));
  }

  public boolean isCasualMatch(final MatchId matchId) {

    return leagueQueryRepository.findByMatchId(matchId).isEmpty()
        && cupQueryRepository.findByMatchId(matchId).isEmpty() && rematchVetoes.stream()
        .noneMatch(veto -> veto.vetoesRematch(matchId));
  }

}
