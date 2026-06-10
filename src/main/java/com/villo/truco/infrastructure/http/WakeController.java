package com.villo.truco.infrastructure.http;

import com.villo.truco.infrastructure.http.dto.response.WakeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público de "wake" pensado para que el front despierte el backend al entrar.
 *
 * <p>En free tier Render apaga el proceso por inactividad. El front pega acá en loop hasta recibir
 * 200: el solo hecho de que el endpoint responda implica que el proceso ya arrancó. Y como Flyway
 * corre las migraciones al boot (antes de aceptar requests HTTP), si esto responde la base también
 * estuvo arriba en el arranque. No hace falta chequear nada más acá: este endpoint responde "¿está
 * el proceso listo?", no "¿está sana cada dependencia?" — eso es responsabilidad de Actuator.
 *
 * <p>Es un contrato propio (no los probes de Actuator) a propósito: Actuator es para la
 * infraestructura (Render/orquestador) y su shape es interno y puede cambiar entre versiones de
 * Spring. Este endpoint le da al front un contrato estable y desacoplado.
 */
@RestController
@RequestMapping("/api/public/wake")
@Tag(name = "Wake", description = "Despertar del backend para el front (cold-start de free tier)")
public class WakeController {

  @GetMapping
  @Operation(summary = "Wake del backend", description = "Devuelve 200 'ready' en cuanto el proceso acepta requests. Sirve para despertar el proceso (cold-start de free tier) desde el front.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Proceso listo", content = @Content(schema = @Schema(implementation = WakeResponse.class)))})
  public ResponseEntity<WakeResponse> wake() {

    return ResponseEntity.ok(WakeResponse.ready());
  }

}
