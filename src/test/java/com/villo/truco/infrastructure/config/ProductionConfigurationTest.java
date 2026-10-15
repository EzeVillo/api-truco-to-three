package com.villo.truco.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.villo.truco.infrastructure.security.TrucoSecurityProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProductionConfigurationTest {

  private static final String TEST_SECRET = "test-only-production-secret-32-bytes";

  @Test
  @DisplayName("Acepta los orígenes nuevos y el anterior durante la migración")
  void acceptsExplicitProductionOrigins() {
    assertDoesNotThrow(() -> new ProductionConfiguration(
        new TrucoWebProperties(List.of("https://ezevillo.com", "https://www.ezevillo.com",
            "https://ezevillo.github.io", "https://localhost", "capacitor://localhost")),
        security(TEST_SECRET)));
  }

  @Test
  @DisplayName("Rechaza el secreto conocido de desarrollo en producción")
  void rejectsDevelopmentSecret() {
    assertThrows(IllegalStateException.class, () -> new ProductionConfiguration(
        new TrucoWebProperties(List.of("https://ezevillo.com")),
        security("truco-local-dev-secret-key-32-bytes")));
  }

  @Test
  @DisplayName("Rechaza una lista vacía de orígenes")
  void rejectsMissingOrigins() {
    assertThrows(IllegalStateException.class, () -> new ProductionConfiguration(
        new TrucoWebProperties(List.of()), security(TEST_SECRET)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"http://localhost:4200", "https://*.netlify.app",
      "https://ezevillo.com/truco-a-3", "https://ezevillo.com/",
      "https://user@ezevillo.com", "https://ezevillo.com?query=1",
      "https://ezevillo.com#fragment", "capacitor://attacker.example", "not an origin"})
  @DisplayName("Rechaza HTTP, comodines y URLs que no representan un origen")
  void rejectsUnsafeOrigins(final String origin) {
    assertThrows(IllegalStateException.class, () -> new ProductionConfiguration(
        new TrucoWebProperties(List.of(origin)), security(TEST_SECRET)));
  }

  private static TrucoSecurityProperties security(final String secret) {
    return new TrucoSecurityProperties(secret, "truco-api", "truco-clients", 900, 604800,
        2592000);
  }
}
