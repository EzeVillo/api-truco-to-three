package com.villo.truco.application.ports;

public interface RetryableTransactionalRunner {

  void run(Runnable action);

}
