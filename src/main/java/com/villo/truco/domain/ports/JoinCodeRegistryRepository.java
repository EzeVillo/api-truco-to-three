package com.villo.truco.domain.ports;

import com.villo.truco.domain.shared.JoinCodeRegistration;

public interface JoinCodeRegistryRepository {

  void save(JoinCodeRegistration registration);

}
