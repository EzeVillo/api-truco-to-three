package com.villo.truco.profile.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.profile.domain.model.AchievementCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetAchievementCatalogQueryHandler")
class GetAchievementCatalogQueryHandlerTest {

  private final GetAchievementCatalogQueryHandler handler = new GetAchievementCatalogQueryHandler();

  @Test
  @DisplayName("devuelve todos los logros de AchievementCode en orden de declaracion")
  void devuelveTodosLosLogrosEnOrden() {

    final var dto = handler.get();

    assertThat(dto.achievements()).containsExactly(AchievementCode.values());
  }

  @Test
  @DisplayName("el catalogo no contiene duplicados")
  void noContieneDuplicados() {

    final var dto = handler.get();

    assertThat(dto.achievements()).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("el catalogo es identico entre llamadas (independiente del jugador)")
  void esIdenticoEntreLlamadas() {

    final List<AchievementCode> primera = handler.get().achievements();
    final List<AchievementCode> segunda = handler.get().achievements();

    assertThat(primera).isEqualTo(segunda);
  }

}
