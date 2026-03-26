package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import com.villo.truco.infrastructure.persistence.mappers.ChatMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataChatRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaChatRepositoryAdapter implements ChatRepository, ChatQueryRepository {

    private final SpringDataChatRepository springDataRepo;
    private final ChatMapper mapper;

    public JpaChatRepositoryAdapter(final SpringDataChatRepository springDataRepo,
        final ChatMapper mapper) {

        this.springDataRepo = springDataRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void save(final Chat chat) {

        try {
            final var entity = this.mapper.toEntity(chat);
            this.springDataRepo.saveAndFlush(entity);
            chat.setVersion(entity.getVersion());
        } catch (final ObjectOptimisticLockingFailureException e) {
            throw new StaleAggregateException("Chat " + chat.getId() + " was modified concurrently",
                e);
        }
    }

    @Override
    @Transactional
    public void delete(final ChatId chatId) {

        this.springDataRepo.deleteById(chatId.value());
    }

    @Override
    public Optional<Chat> findById(final ChatId chatId) {

        return this.springDataRepo.findById(chatId.value()).map(this.mapper::toDomain);
    }

    @Override
    public Optional<Chat> findByParentTypeAndParentId(final ChatParentType parentType,
        final String parentId) {

        return this.springDataRepo.findByParentTypeAndParentId(parentType.name(), parentId)
            .map(this.mapper::toDomain);
    }

}
