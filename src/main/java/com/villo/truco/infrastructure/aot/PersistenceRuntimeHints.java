package com.villo.truco.infrastructure.aot;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * Hints de reflection que Hibernate necesita en runtime y que Spring AOT no registra.
 *
 * <p>Al construir el {@code SessionFactory}, {@code MultiIdEntityLoaderArrayParam} instancia
 * reflexivamente un array del tipo del id de cada entidad (p.ej. {@code UUID[]}) para los loaders
 * multi-id; sin estos hints el binario nativo falla al crear el {@code entityManagerFactory}.
 * Escanea todas las {@code @Entity} en build-time y registra el array del tipo de cada campo
 * {@code @Id}, por lo que cubre automáticamente entidades e ids nuevos.
 */
public final class PersistenceRuntimeHints implements RuntimeHintsRegistrar {

  private static final String BASE_PACKAGE = "com.villo.truco";

  @Override
  public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {

    final var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
    for (final var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
      var entityClass = ClassUtils.resolveClassName(candidate.getBeanClassName(), classLoader);
      while (entityClass != null && entityClass != Object.class) {
        for (final var field : entityClass.getDeclaredFields()) {
          if (field.isAnnotationPresent(Id.class)) {
            hints.reflection().registerType(field.getType().arrayType());
          }
        }
        entityClass = entityClass.getSuperclass();
      }
    }
  }

}
