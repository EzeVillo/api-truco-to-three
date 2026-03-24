package com.villo.truco.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(TrucoSecurityProperties.class)
public class SecurityConfiguration {

  private static final int MIN_HS256_SECRET_BYTES = 32;

  @Bean
  SecretKey jwtSecretKey(final TrucoSecurityProperties properties) {

    final byte[] secretBytes = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < MIN_HS256_SECRET_BYTES) {
      throw new IllegalStateException(
          "Property truco.security.jwt-secret must be at least 32 bytes (256 bits) for HS256");
    }

    return new SecretKeySpec(secretBytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(final SecretKey jwtSecretKey) {

    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
  }

  @Bean
  JwtDecoder jwtDecoder(final SecretKey jwtSecretKey, final TrucoSecurityProperties properties) {

    final var decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
        .macAlgorithm(MacAlgorithm.HS256).build();

    final var validator = new DelegatingOAuth2TokenValidator<>(
        new JwtTimestampValidator(Duration.ofSeconds(30)),
        this.issuerValidator(properties.issuer()), this.audienceValidator(properties.audience()));

    decoder.setJwtValidator(validator);

    return decoder;
  }

  @Bean
  SecurityFilterChain securityFilterChain(final HttpSecurity http) {

    return http.cors(Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/actuator/health/**").permitAll()
            .requestMatchers("/actuator/**").authenticated()
            .requestMatchers("/api/**").authenticated().anyRequest().permitAll())
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())).build();
  }

  private OAuth2TokenValidator<Jwt> issuerValidator(final String issuer) {

    return jwt -> issuer.equals(jwt.getClaimAsString("iss")) ? OAuth2TokenValidatorResult.success()
        : OAuth2TokenValidatorResult.failure(
            new org.springframework.security.oauth2.core.OAuth2Error("invalid_token",
                "Invalid issuer", null));
  }

  private OAuth2TokenValidator<Jwt> audienceValidator(final String audience) {

    return jwt -> jwt.getAudience().contains(audience) ? OAuth2TokenValidatorResult.success()
        : OAuth2TokenValidatorResult.failure(
            new org.springframework.security.oauth2.core.OAuth2Error("invalid_token",
                "Invalid audience", null));
  }

}
