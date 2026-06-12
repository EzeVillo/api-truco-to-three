package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.BotProfileDTO;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.HiddenBotIdsProvider;
import com.villo.truco.application.ports.in.GetBotsUseCase;
import com.villo.truco.application.queries.GetBotsQuery;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GetBotsQueryHandler implements GetBotsUseCase {

  private final BotRegistry botRegistry;
  private final List<HiddenBotIdsProvider> hiddenBotIdsProviders;

  public GetBotsQueryHandler(final BotRegistry botRegistry,
      final List<HiddenBotIdsProvider> hiddenBotIdsProviders) {

    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.hiddenBotIdsProviders = List.copyOf(Objects.requireNonNull(hiddenBotIdsProviders));
  }

  @Override
  public List<BotProfileDTO> handle(final GetBotsQuery query) {

    final var hiddenBotIds = this.hiddenBotIdsProviders.stream()
        .flatMap(provider -> provider.hiddenBotIds().stream())
        .collect(Collectors.toUnmodifiableSet());

    return this.botRegistry.getAll().stream()
        .filter(profile -> !hiddenBotIds.contains(profile.playerId())).map(BotProfileDTO::of)
        .toList();
  }

}
