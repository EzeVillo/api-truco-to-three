package com.villo.truco.application.ports;

public interface PasswordHasher {

  String hash(String rawPassword);

  boolean matches(String rawPassword, String hashedPassword);

}
