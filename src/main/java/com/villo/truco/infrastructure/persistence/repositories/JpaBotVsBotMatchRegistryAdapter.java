package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.entities.BotVsBotMatchJpaEntity;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataBotVsBotMatchRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaBotVsBotMatchRegistryAdapter implements BotVsBotMatchRegistry {

  private final SpringDataBotVsBotMatchRepository springDataBotVsBotMatchRepository;

  public JpaBotVsBotMatchRegistryAdapter(
      final SpringDataBotVsBotMatchRepository springDataBotVsBotMatchRepository) {

    this.springDataBotVsBotMatchRepository = springDataBotVsBotMatchRepository;
  }

  @Override
  @Transactional
  public void register(final MatchId matchId, final PlayerId ownerId) {

    final var entity = new BotVsBotMatchJpaEntity();
    entity.setMatchId(matchId.value());
    entity.setOwnerId(ownerId.value());
    this.springDataBotVsBotMatchRepository.save(entity);
  }

  @Override
  public boolean isBotVsBotMatch(final MatchId matchId) {

    return this.springDataBotVsBotMatchRepository.existsById(matchId.value());
  }

  @Override
  public Optional<PlayerId> findOwnerByMatchId(final MatchId matchId) {

    return this.springDataBotVsBotMatchRepository.findById(matchId.value())
        .map(entity -> new PlayerId(entity.getOwnerId()));
  }

  @Override
  public Optional<MatchId> findActiveOwnedMatchId(final PlayerId ownerId) {

    return this.springDataBotVsBotMatchRepository.findActiveOwnedMatchIds(ownerId.value()).stream()
        .findFirst().map(MatchId::new);
  }

}
