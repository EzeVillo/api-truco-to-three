package com.villo.truco.auth.infrastructure.persistence.repositories.spring;

import java.util.UUID;

public interface UserUsernameProjection {

  UUID getId();

  String getUsername();

}
