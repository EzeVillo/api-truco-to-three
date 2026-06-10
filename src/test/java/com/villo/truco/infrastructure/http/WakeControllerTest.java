package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.infrastructure.http.dto.response.WakeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("WakeController")
class WakeControllerTest {

  private final WakeController controller = new WakeController();

  @Test
  @DisplayName("GET wake devuelve 200 ready")
  void returnsReady() {

    final var response = controller.wake();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(WakeResponse.READY);
  }

}
