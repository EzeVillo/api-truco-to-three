package com.villo.truco.application.ports.out.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class TimeoutKeyTest {

  @Test
  void igualdad_por_valor_no_por_referencia() {

    final var key1 = new TimeoutKey(EntityType.MATCH, "abc-123");
    final var key2 = new TimeoutKey(EntityType.MATCH, "abc-123");

    assertNotSame(key1, key2);
    assertEquals(key1, key2);
    assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void keys_distintas_por_tipo_no_son_iguales() {

    final var key1 = new TimeoutKey(EntityType.MATCH, "abc-123");
    final var key2 = new TimeoutKey(EntityType.CUP, "abc-123");

    assertNotEquals(key1, key2);
  }

  @Test
  void keys_distintas_por_id_no_son_iguales() {

    final var key1 = new TimeoutKey(EntityType.MATCH, "abc-123");
    final var key2 = new TimeoutKey(EntityType.MATCH, "xyz-456");

    assertNotEquals(key1, key2);
  }

}
