package com.villo.truco.domain.model.bot;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.bot.exceptions.BotWithoutCardsException;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CardSelectionPolicyTest {

  private static final BotPersonality BALANCED = new BotPersonality(50, 50, 50, 50, 50);

  @Test
  void select_withoutCards_throwsBotWithoutCardsException() {

    final var policy = new CardSelectionPolicy(BALANCED, new Random());

    assertThatThrownBy(() -> policy.select(List.of(), null)).isInstanceOf(
        BotWithoutCardsException.class).hasMessage("Bot has no cards to play");
  }

}
