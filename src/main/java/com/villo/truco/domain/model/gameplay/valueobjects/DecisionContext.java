package com.villo.truco.domain.model.gameplay.valueobjects;

import com.villo.truco.domain.model.match.MatchPlayerDecisionView;
import java.util.List;
import java.util.Objects;

public record DecisionContext(int scoreActorBefore, int scoreActorAfter, int scoreOppBefore,
                              int scoreOppAfter, int gamesWonActor, int gamesWonOpp, int tantosActor,
                              int tantosOpp, boolean isMano, ActorSeat manoSeat, ActorSeat turnSeat,
                              boolean forced, List<String> availableActions,
                              boolean quieroYMeVoyDisponible, boolean puedeIrseAlMazo,
                              boolean envidoDisponible, MatchPlayerDecisionView observable) {

  public DecisionContext {

    availableActions = List.copyOf(Objects.requireNonNull(availableActions, "availableActions"));
    Objects.requireNonNull(observable, "observable");
  }

}
