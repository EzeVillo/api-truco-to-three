package com.villo.truco.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(TrucoWebProperties.class)
public class CorsConfig {

  private final TrucoWebProperties webProperties;

  public CorsConfig(final TrucoWebProperties webProperties) {

    this.webProperties = webProperties;
  }

  /**
   * Fuente de CORS única que toma {@code SecurityFilterChain} vía {@code .cors(withDefaults())}.
   *
   * <p>Se expone como bean (nombre {@code corsConfigurationSource}, que Spring Security busca por
   * convención) en lugar de configurarlo con {@code WebMvcConfigurer#addCorsMappings}. El motivo:
   * {@code addCorsMappings} solo aplica al {@code RequestMappingHandlerMapping} de los controllers
   * ({@code /api/**}); los endpoints de Actuator ({@code /actuator/**}) los sirve otro handler
   * mapping con su propia config de CORS ({@code management.endpoints.web.cors.*}), por lo que
   * quedaban sin el header {@code Access-Control-Allow-Origin} y el browser bloqueaba el readiness
   * probe del front aunque el server respondiera 200. El {@code CorsFilter} de Security corre antes
   * del dispatcher y cubre todas las rutas, incluido {@code /actuator/**}.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {

    final CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(this.webProperties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

}
