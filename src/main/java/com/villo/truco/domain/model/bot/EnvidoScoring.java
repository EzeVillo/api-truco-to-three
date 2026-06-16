package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import java.util.List;

public interface EnvidoScoring {

  int of(List<Card> cards);

}
