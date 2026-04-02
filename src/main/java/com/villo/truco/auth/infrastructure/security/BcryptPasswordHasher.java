package com.villo.truco.auth.infrastructure.security;

import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public final class BcryptPasswordHasher implements PasswordHasher {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @Override
  public HashedPassword hash(final RawPassword rawPassword) {

    return new HashedPassword(this.encoder.encode(rawPassword.value()));
  }

  @Override
  public boolean matches(final String rawPassword, final HashedPassword hashedPassword) {

    return this.encoder.matches(rawPassword, hashedPassword.value());
  }

}
