package com.villo.truco.support;

import jakarta.persistence.EntityManagerFactory;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

public final class QueryCounter {

  private final Statistics statistics;

  public QueryCounter(final EntityManagerFactory entityManagerFactory) {

    this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    if (!this.statistics.isStatisticsEnabled()) {
      throw new IllegalStateException("Las estadisticas de Hibernate estan deshabilitadas. Activa "
          + "spring.jpa.properties.hibernate.generate_statistics=true en el perfil de test.");
    }
  }

  public long count(final Runnable action) {

    return countReturning(() -> {
      action.run();
      return null;
    }).queryCount();
  }

  public <T> Counted<T> countReturning(final Supplier<T> action) {

    this.statistics.clear();
    final var result = action.get();
    return new Counted<>(result, this.statistics.getPrepareStatementCount());
  }

  public record Counted<T>(T result, long queryCount) {

  }

}
