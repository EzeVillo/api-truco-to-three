package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.BotProfileDTO;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.GetBotsUseCase;
import com.villo.truco.application.queries.GetBotsQuery;
import java.util.List;
import java.util.Objects;

public final class GetBotsQueryHandler implements GetBotsUseCase {

  private final BotRegistry botRegistry;

  public GetBotsQueryHandler(final BotRegistry botRegistry) {

    this.botRegistry = Objects.requireNonNull(botRegistry);
  }

  @Override
  public List<BotProfileDTO> handle(final GetBotsQuery query) {

    return this.botRegistry.getAll().stream().map(BotProfileDTO::of).toList();
  }

}
