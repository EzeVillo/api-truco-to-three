package com.villo.truco.infrastructure.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("TransactionalBehavior")
class TransactionalBehaviorTest {

  private TransactionTemplate transactionTemplate;
  private TransactionalBehavior behavior;

  @BeforeEach
  void setUp() {

    transactionTemplate = mock(TransactionTemplate.class);
    behavior = new TransactionalBehavior(transactionTemplate);
  }

  @Test
  @DisplayName("ejecuta el supplier dentro de la transacción y retorna el resultado")
  void executesSupplierInsideTransaction() {

    when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
      TransactionCallback<?> callback = invocation.getArgument(0);
      return callback.doInTransaction(null);
    });

    final var result = behavior.handle("cmd", () -> "result");

    assertThat(result).isEqualTo("result");
    verify(transactionTemplate).execute(any());
  }

  @Test
  @DisplayName("retorna null cuando el supplier retorna null (soporte para handlers Void)")
  void returnsNullWhenSupplierReturnsNull() {

    when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
      TransactionCallback<?> callback = invocation.getArgument(0);
      return callback.doInTransaction(null);
    });

    final var result = behavior.handle("cmd", () -> null);

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("propaga excepciones lanzadas por el supplier")
  void propagatesExceptionsFromSupplier() {

    when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
      TransactionCallback<?> callback = invocation.getArgument(0);
      return callback.doInTransaction(null);
    });

    final var cause = new RuntimeException("boom");

    assertThatThrownBy(() -> behavior.handle("cmd", () -> {
      throw cause;
    })).isSameAs(cause);
  }

}
