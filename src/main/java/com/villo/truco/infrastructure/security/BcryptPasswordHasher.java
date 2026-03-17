package com.villo.truco.infrastructure.security;

import com.villo.truco.application.ports.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public final class BcryptPasswordHasher implements PasswordHasher {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @Override
  public String hash(final String rawPassword) {

    return this.encoder.encode(rawPassword);
  }

  @Override
  public boolean matches(final String rawPassword, final String hashedPassword) {

    return this.encoder.matches(rawPassword, hashedPassword);
  }

}
