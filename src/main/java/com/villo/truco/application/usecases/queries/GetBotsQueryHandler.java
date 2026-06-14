package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.BotCatalogDTO;
import com.villo.truco.application.dto.BotProfileDTO;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.HiddenBotIdsProvider;
import com.villo.truco.application.ports.RevealedBotIdsProvider;
import com.villo.truco.application.ports.in.GetBotsUseCase;
import com.villo.truco.application.queries.GetBotsQuery;
import com.villo.truco.domain.model.bot.BotProfile;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class GetBotsQueryHandler implements GetBotsUseCase {

  private final BotRegistry botRegistry;
  private final List<HiddenBotIdsProvider> hiddenBotIdsProviders;
  private final List<RevealedBotIdsProvider> revealedBotIdsProviders;

  public GetBotsQueryHandler(final BotRegistry botRegistry,
      final List<HiddenBotIdsProvider> hiddenBotIdsProviders,
      final List<RevealedBotIdsProvider> revealedBotIdsProviders) {

    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.hiddenBotIdsProviders = List.copyOf(Objects.requireNonNull(hiddenBotIdsProviders));
    this.revealedBotIdsProviders = List.copyOf(Objects.requireNonNull(revealedBotIdsProviders));
  }

  @Override
  public BotCatalogDTO handle(final GetBotsQuery query) {

    final var hiddenBotIds = this.hiddenBotIdsProviders.stream()
        .flatMap(provider -> provider.hiddenBotIds().stream())
        .collect(Collectors.toUnmodifiableSet());

    final var revealedBotIds = this.revealedBotIdsProviders.stream()
        .flatMap(provider -> provider.revealedBotIds(query.playerId()).stream())
        .collect(Collectors.toUnmodifiableSet());

    final var casual = this.profilesMatching(profile -> !hiddenBotIds.contains(profile.playerId()));
    final var campaignUnlocked = this.profilesMatching(
        profile -> revealedBotIds.contains(profile.playerId()));

    return new BotCatalogDTO(casual, campaignUnlocked);
  }

  private List<BotProfileDTO> profilesMatching(final Predicate<BotProfile> predicate) {

    return this.botRegistry.getAll().stream().filter(predicate).map(BotProfileDTO::of).toList();
  }

}
