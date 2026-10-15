package com.villo.truco.infrastructure.config;

import com.villo.truco.infrastructure.security.TrucoSecurityProperties;
import java.net.URI;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Valida el perímetro del despliegue antes de aceptar tráfico de producción. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionConfiguration {

  public ProductionConfiguration(final TrucoWebProperties webProperties,
      final TrucoSecurityProperties securityProperties) {

    if ("truco-local-dev-secret-key-32-bytes".equals(securityProperties.jwtSecret())) {
      throw new IllegalStateException("TRUCO_JWT_SECRET must not use the development secret");
    }

    if (webProperties.allowedOrigins() == null || webProperties.allowedOrigins().isEmpty()) {
      throw new IllegalStateException("TRUCO_ALLOWED_ORIGINS must contain explicit allowed origins");
    }

    for (final var origin : webProperties.allowedOrigins()) {
      final URI uri;
      try {
        uri = URI.create(origin);
      } catch (final IllegalArgumentException exception) {
        throw new IllegalStateException("TRUCO_ALLOWED_ORIGINS must contain valid origins");
      }
      // Capacitor usa este origen fijo en iOS; Android usa https://localhost.
      final boolean nativeOrigin = "capacitor://localhost".equals(origin);
      if ((!"https".equals(uri.getScheme()) && !nativeOrigin) || uri.getHost() == null
          || uri.getUserInfo() != null || !uri.getPath().isEmpty()
          || uri.getQuery() != null || uri.getFragment() != null || origin.contains("*")) {
        throw new IllegalStateException(
            "TRUCO_ALLOWED_ORIGINS must contain HTTPS origins or the exact Capacitor origin, without paths");
      }
    }
  }
}
