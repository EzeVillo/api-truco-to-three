package com.villo.truco.testutil;

import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registro de autoria bot-vs-bot en memoria para tests. Sin filtro de estado: "activo" es
 * cualquier match registrado, salvo que se marque como finalizado con {@link #finish(MatchId)}.
 */
public final class InMemoryBotVsBotMatchRegistry implements BotVsBotMatchRegistry {

  private final Map<MatchId, PlayerId> owners = new LinkedHashMap<>();
  private final Map<MatchId, Boolean> active = new LinkedHashMap<>();

  @Override
  public void register(final MatchId matchId, final PlayerId ownerId) {

    this.owners.put(matchId, ownerId);
    this.active.put(matchId, true);
  }

  public void finish(final MatchId matchId) {

    this.active.put(matchId, false);
  }

  @Override
  public boolean isBotVsBotMatch(final MatchId matchId) {

    return this.owners.containsKey(matchId);
  }

  @Override
  public Optional<PlayerId> findOwnerByMatchId(final MatchId matchId) {

    return Optional.ofNullable(this.owners.get(matchId));
  }

  @Override
  public Optional<MatchId> findActiveOwnedMatchId(final PlayerId ownerId) {

    return this.owners.entrySet().stream()
        .filter(entry -> entry.getValue().equals(ownerId))
        .filter(entry -> this.active.getOrDefault(entry.getKey(), false))
        .map(Map.Entry::getKey)
        .findFirst();
  }

}
