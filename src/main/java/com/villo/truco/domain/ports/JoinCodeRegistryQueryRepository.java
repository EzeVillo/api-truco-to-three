package com.villo.truco.domain.ports;

import com.villo.truco.domain.shared.JoinCodeRegistration;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import java.util.Optional;

public interface JoinCodeRegistryQueryRepository {

  Optional<JoinCodeRegistration> findByJoinCode(JoinCode joinCode);

}
