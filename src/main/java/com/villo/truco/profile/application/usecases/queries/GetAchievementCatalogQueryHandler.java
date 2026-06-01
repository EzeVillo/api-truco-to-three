package com.villo.truco.profile.application.usecases.queries;

import com.villo.truco.profile.application.dto.AchievementCatalogDTO;
import com.villo.truco.profile.domain.model.AchievementCode;
import java.util.List;

public final class GetAchievementCatalogQueryHandler implements GetAchievementCatalogUseCase {

  @Override
  public AchievementCatalogDTO get() {

    return new AchievementCatalogDTO(List.of(AchievementCode.values()));
  }

}
