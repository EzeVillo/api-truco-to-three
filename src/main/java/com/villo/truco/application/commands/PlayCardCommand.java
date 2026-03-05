package com.villo.truco.application.commands;

import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.match.valueobjects.Suit;
import java.util.Objects;

public record PlayCardCommand(MatchId matchId, PlayerId playerId, Card card) {

  public PlayCardCommand {

    Objects.requireNonNull(matchId);
    Objects.requireNonNull(playerId);
    Objects.requireNonNull(card);
  }

  public PlayCardCommand(String matchId, String playerId, String suit, int number) {

    this(MatchId.of(matchId), PlayerId.of(playerId), Card.of(Suit.valueOf(suit), number));
  }

}
