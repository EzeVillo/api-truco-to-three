package com.villo.truco.auth.domain.ports;

import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;

public interface PasswordHasher {

  HashedPassword hash(RawPassword rawPassword);

  boolean matches(String rawPassword, HashedPassword hashedPassword);

}
