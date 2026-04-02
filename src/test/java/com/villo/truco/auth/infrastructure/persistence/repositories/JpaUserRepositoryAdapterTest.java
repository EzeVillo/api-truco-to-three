package com.villo.truco.auth.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.model.user.User;
import com.villo.truco.auth.domain.model.user.exceptions.UsernameUnavailableException;
import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.auth.infrastructure.persistence.entities.UserJpaEntity;
import com.villo.truco.auth.infrastructure.persistence.mappers.UserMapper;
import com.villo.truco.auth.infrastructure.persistence.repositories.spring.SpringDataUserRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

@DisplayName("JpaUserRepositoryAdapter")
class JpaUserRepositoryAdapterTest {

  @Test
  @DisplayName("delega existsByUsername y findByUsername")
  void delegatesQueries() {

    final var springRepo = mock(SpringDataUserRepository.class);
    final var adapter = new JpaUserRepositoryAdapter(springRepo, mock(UserMapper.class));

    adapter.existsByUsername(new Username("user"));
    adapter.findByUsername(new Username("user"));

    verify(springRepo).existsByUsername("user");
    verify(springRepo).findByUsername("user");
  }

  @Test
  @DisplayName("traduce unique constraint de username a excepcion de dominio")
  void translatesUsernameUniqueConstraintViolation() {

    final var springRepo = mock(SpringDataUserRepository.class);
    final var mapper = mock(UserMapper.class);
    final var adapter = new JpaUserRepositoryAdapter(springRepo, mapper);
    final var user = new User(PlayerId.generate(), new Username("juancho"),
        new com.villo.truco.auth.domain.model.user.valueobjects.RawPassword("Clave1!"),
        new PasswordHasher() {
          @Override
          public HashedPassword hash(
              final com.villo.truco.auth.domain.model.user.valueobjects.RawPassword rawPassword) {

            return new HashedPassword("hashed:" + rawPassword.value());
          }

          @Override
          public boolean matches(final String rawPassword, final HashedPassword hashedPassword) {

            return hashedPassword.value().equals("hashed:" + rawPassword);
          }
        });
    final var entity = new UserJpaEntity();
    when(mapper.toEntity(user)).thenReturn(entity);
    doThrow(new DataIntegrityViolationException("duplicate username",
        new ConstraintViolationException("duplicate username", new SQLException("duplicate"),
            UserJpaEntity.USERNAME_UNIQUE_CONSTRAINT))).when(springRepo).saveAndFlush(entity);

    assertThatThrownBy(() -> adapter.saveEnsuringUsernameAvailable(user)).isInstanceOf(
        UsernameUnavailableException.class).hasMessage("Username already taken: juancho");
  }

  @Test
  @DisplayName("repropaga violaciones de integridad no relacionadas a username")
  void rethrowsUnrelatedIntegrityViolation() {

    final var springRepo = mock(SpringDataUserRepository.class);
    final var mapper = mock(UserMapper.class);
    final var adapter = new JpaUserRepositoryAdapter(springRepo, mapper);
    final var user = new User(PlayerId.generate(), new Username("juancho"),
        new com.villo.truco.auth.domain.model.user.valueobjects.RawPassword("Clave1!"),
        new PasswordHasher() {
          @Override
          public HashedPassword hash(
              final com.villo.truco.auth.domain.model.user.valueobjects.RawPassword rawPassword) {

            return new HashedPassword("hashed:" + rawPassword.value());
          }

          @Override
          public boolean matches(final String rawPassword, final HashedPassword hashedPassword) {

            return hashedPassword.value().equals("hashed:" + rawPassword);
          }
        });
    final var entity = new UserJpaEntity();
    final var ex = new DataIntegrityViolationException("other integrity violation",
        new ConstraintViolationException("other integrity violation", new SQLException("other"),
            "uk_users_other_column"));
    when(mapper.toEntity(user)).thenReturn(entity);
    doThrow(ex).when(springRepo).saveAndFlush(entity);

    assertThatThrownBy(() -> adapter.saveEnsuringUsernameAvailable(user)).isSameAs(ex);
  }

}
