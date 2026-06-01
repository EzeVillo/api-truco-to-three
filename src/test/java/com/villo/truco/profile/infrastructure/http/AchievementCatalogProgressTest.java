package com.villo.truco.profile.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.profile.application.usecases.queries.GetAchievementCatalogQueryHandler;
import com.villo.truco.profile.domain.model.AchievementCode;
import com.villo.truco.profile.infrastructure.http.dto.response.AchievementCatalogResponse;
import com.villo.truco.profile.infrastructure.http.dto.response.AchievementCatalogResponse.AchievementCatalogItemResponse;
import com.villo.truco.profile.infrastructure.http.dto.response.UnlockedAchievementResponse;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Merge catalogo + perfil (US2)")
class AchievementCatalogProgressTest {

  @Test
  @DisplayName("todo logro desbloqueable aparece en el catalogo, asi el merge por achievementCode nunca deja un desbloqueado huerfano")
  void todoLogroDesbloqueableEstaEnElCatalogo() {

    final var catalog = AchievementCatalogResponse.from(
        new GetAchievementCatalogQueryHandler().get());
    final List<String> catalogCodes = catalog.achievements().stream()
        .map(AchievementCatalogItemResponse::achievementCode).toList();

    final List<String> desbloqueables = Arrays.stream(AchievementCode.values()).map(Enum::name)
        .toList();

    assertThat(catalogCodes).containsAll(desbloqueables);
  }

  @Test
  @DisplayName("catalogo y perfil usan el mismo nombre de campo achievementCode para permitir el merge")
  void catalogoYPerfilCompartenLaClaveDeMerge() throws NoSuchFieldException {

    final Field catalogField = AchievementCatalogItemResponse.class.getDeclaredField(
        "achievementCode");
    final Field profileField = UnlockedAchievementResponse.class.getDeclaredField("achievementCode");

    assertThat(catalogField.getType()).isEqualTo(String.class);
    assertThat(profileField.getType()).isEqualTo(String.class);
  }

}
