# syntax=docker/dockerfile:1

###############################################################################
# Stage 1 — Compilar el bootJar
###############################################################################
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copiamos primero lo que cambia poco para cachear la descarga de dependencias.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Ahora el código fuente y el empaquetado (sin tests: el CI ya los corre aparte).
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test \
    && cp build/libs/*.jar /app.jar

###############################################################################
# Stage 2 — Construir un JRE mínimo a medida con jlink
###############################################################################
FROM eclipse-temurin:21-jdk-alpine AS jre-build
RUN "$JAVA_HOME/bin/jlink" \
      --add-modules \
java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.management,jdk.unsupported,jdk.httpserver \
      --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
      --output /javaruntime

###############################################################################
# Stage 3 — Imagen final mínima (Alpine + JRE recortado + fat jar)
###############################################################################
FROM alpine:3.20
# tzdata: la app opera con zona America/Argentina/Buenos_Aires.
RUN apk add --no-cache tzdata \
    && addgroup -S spring && adduser -S spring -G spring

ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

WORKDIR /app
# Ejecutamos el fat jar directamente: el Main-Class del manifest resuelve el
# launcher embebido, sin depender de la ubicación de las capas extraídas.
COPY --from=build /app.jar ./app.jar

USER spring:spring
EXPOSE 8080

# Tuneo para free tier (~512 MB): heap acotado, GC liviano y corte ante OOM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=3s --start-period=70s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
