package com.villo.truco.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(TrucoWebProperties.class)
public class CorsConfig implements WebMvcConfigurer {

  private final TrucoWebProperties webProperties;

  public CorsConfig(final TrucoWebProperties webProperties) {

    this.webProperties = webProperties;
  }

  @Override
  public void addCorsMappings(final CorsRegistry registry) {

    registry.addMapping("/**").allowedOriginPatterns(this.webProperties.allowedOriginsArray())
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*");
  }

}
