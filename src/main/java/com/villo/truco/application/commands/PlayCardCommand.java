package com.villo.truco.application.commands;

import com.villo.truco.application.shared.EnumArgumentParser;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record PlayCardCommand(MatchId matchId, PlayerId playerId, Card card) {

  public PlayCardCommand {

    Objects.requireNonNull(matchId);
    Objects.requireNonNull(playerId);
    Objects.requireNonNull(card);
  }

  public PlayCardCommand(String matchId, String playerId, String suit, int number) {

    this(MatchId.of(matchId), PlayerId.of(playerId),
        Card.of(EnumArgumentParser.parse(Suit.class, "suit", suit), number));
  }

}
