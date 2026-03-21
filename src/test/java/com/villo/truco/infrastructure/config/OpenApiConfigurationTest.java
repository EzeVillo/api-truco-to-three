package com.villo.truco.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiConfigurationTest {

  @Test
  void buildsOpenApiInfo() {

    final var openApi = new OpenApiConfiguration().openAPI();
    assertThat(openApi.getInfo().getTitle()).isEqualTo("Truco API");
  }

}
