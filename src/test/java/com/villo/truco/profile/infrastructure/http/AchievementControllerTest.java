package com.villo.truco.profile.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.profile.application.dto.AchievementCatalogDTO;
import com.villo.truco.profile.application.usecases.queries.GetAchievementCatalogUseCase;
import com.villo.truco.profile.domain.model.AchievementCode;
import com.villo.truco.profile.infrastructure.http.dto.response.AchievementCatalogResponse.AchievementCatalogItemResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("AchievementController")
class AchievementControllerTest {

  @Test
  @DisplayName("GET /api/achievements devuelve 200 con todos los codigos del catalogo")
  void getCatalogReturns200() {

    final var useCase = mock(GetAchievementCatalogUseCase.class);
    final var controller = new AchievementController(useCase);
    when(useCase.get()).thenReturn(new AchievementCatalogDTO(List.of(AchievementCode.values())));

    final var response = controller.getCatalog();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    final var codes = response.getBody().achievements().stream()
        .map(AchievementCatalogItemResponse::achievementCode).toList();
    assertThat(codes).containsExactly(
        Arrays.stream(AchievementCode.values()).map(Enum::name).toArray(String[]::new));
  }

  @Test
  @DisplayName("el catalogo expone solo el codigo, sin titulo ni descripcion ni estado")
  void exposesOnlyCode() {

    final var useCase = mock(GetAchievementCatalogUseCase.class);
    final var controller = new AchievementController(useCase);
    when(useCase.get()).thenReturn(
        new AchievementCatalogDTO(List.of(AchievementCode.FOLD_BEFORE_ANY_CARD_IS_PLAYED)));

    final var response = controller.getCatalog();

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().achievements()).hasSize(1);
    final var item = response.getBody().achievements().getFirst();
    assertThat(item.achievementCode()).isEqualTo("FOLD_BEFORE_ANY_CARD_IS_PLAYED");
    assertThat(item.getClass().getRecordComponents()).hasSize(1);
  }

}
