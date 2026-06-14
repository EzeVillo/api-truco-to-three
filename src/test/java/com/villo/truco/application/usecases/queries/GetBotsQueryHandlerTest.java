package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.dto.BotProfileDTO;
import com.villo.truco.application.ports.HiddenBotIdsProvider;
import com.villo.truco.application.ports.RevealedBotIdsProvider;
import com.villo.truco.application.queries.GetBotsQuery;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.bot.InMemoryBotRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetBotsQueryHandler")
class GetBotsQueryHandlerTest {

  private final PlayerId player = PlayerId.generate();
  private final PlayerId casualBot = PlayerId.generate();
  private final PlayerId campaignBot = PlayerId.generate();
  private final InMemoryBotRegistry botRegistry = new InMemoryBotRegistry();

  GetBotsQueryHandlerTest() {

    this.botRegistry.register(botProfile(this.casualBot, "Casual"));
    this.botRegistry.register(botProfile(this.campaignBot, "Campaña"));
  }

  private static BotProfile botProfile(final PlayerId playerId, final String name) {

    return new BotProfile(playerId, name, new BotPersonality(50, 50, 50, 50, 50));
  }

  @Test
  @DisplayName("excluye del listado casual los bots ocultos y separa los desbloqueados del jugador")
  void splitsCasualAndCampaignUnlocked() {

    final HiddenBotIdsProvider hidden = () -> Set.of(this.campaignBot);
    final RevealedBotIdsProvider revealed = ignored -> Set.of(this.campaignBot);
    final var handler = new GetBotsQueryHandler(this.botRegistry, List.of(hidden),
        List.of(revealed));

    final var catalog = handler.handle(new GetBotsQuery(this.player));

    assertThat(catalog.casual()).extracting(BotProfileDTO::botId)
        .containsExactly(this.casualBot.value().toString());
    assertThat(catalog.campaignUnlocked()).extracting(BotProfileDTO::botId)
        .containsExactly(this.campaignBot.value().toString());
  }

  @Test
  @DisplayName("sin bots desbloqueados, campaignUnlocked queda vacío y los de campaña no aparecen en casual")
  void noRevealedBotsLeavesCampaignUnlockedEmpty() {

    final HiddenBotIdsProvider hidden = () -> Set.of(this.campaignBot);
    final RevealedBotIdsProvider revealed = ignored -> Set.of();
    final var handler = new GetBotsQueryHandler(this.botRegistry, List.of(hidden),
        List.of(revealed));

    final var catalog = handler.handle(new GetBotsQuery(this.player));

    assertThat(catalog.casual()).extracting(BotProfileDTO::botId)
        .containsExactly(this.casualBot.value().toString());
    assertThat(catalog.campaignUnlocked()).isEmpty();
  }

}
