package com.villo.truco.campaign.infrastructure.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.BotProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CampaignBotCatalog")
class CampaignBotCatalogTest {

  private final CampaignBotCatalog catalog = new CampaignBotCatalog();

  @Test
  @DisplayName("construye una escalera válida de 100 bots con puntos estrictamente decrecientes")
  void buildsValidLadderOfHundredBots() {

    final var ladder = catalog.ladder();

    assertThat(ladder.totalBots()).isEqualTo(100);
    assertThat(ladder.bots().getFirst().position()).isEqualTo(1);
    assertThat(ladder.bots().getLast().position()).isEqualTo(100);
  }

  @Test
  @DisplayName("el catálogo es determinista: dos instancias producen los mismos bots")
  void catalogIsDeterministic() {

    final var other = new CampaignBotCatalog();

    assertThat(catalog.ladder().bots()).isEqualTo(other.ladder().bots());
    assertThat(catalog.botProfiles().stream().map(BotProfile::personality).toList()).isEqualTo(
        other.botProfiles().stream().map(BotProfile::personality).toList());
  }

  @Test
  @DisplayName("expone un perfil de bot por cada posición de la escalera")
  void exposesOneBotProfilePerLadderPosition() {

    assertThat(catalog.botProfiles()).hasSize(catalog.ladder().totalBots());
  }

  @Test
  @DisplayName("alcanzar el puesto 1 requiere los puntos de la cima (~24 h de juego efectivo)")
  void topPositionRequiresTheConfiguredPointBudget() {

    assertThat(catalog.ladder().bots().getFirst().points()).isEqualTo(43_200);
  }

}
