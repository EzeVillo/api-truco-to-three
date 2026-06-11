# syntax=docker/dockerfile:1
#
# Empaqueta el binario nativo compilado por GraalVM en CI (GitHub Actions).
# NO compila: el `nativeCompile` corre en el runner (necesita 4+ vCPU / ~8 GB RAM,
# inviable en el build de Render o en un stage de Docker en free tier).
# El workflow .github/workflows/release.yml construye esta imagen pasando
# el binario ya compilado en build/native/nativeCompile/.
#
# El binario se compila en ubuntu-latest (glibc), por eso la base es debian slim
# y no alpine (musl).

FROM debian:bookworm-slim

# tzdata: la app opera con zona America/Argentina/Buenos_Aires.
# ca-certificates: HTTPS saliente (JWKs, webhooks, etc.).
RUN apt-get update \
    && apt-get install -y --no-install-recommends tzdata ca-certificates wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app
COPY build/native/nativeCompile/truco ./truco
RUN chmod +x ./truco && chown -R spring:spring /app

USER spring:spring
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

# Sin JIT ni heap dinámico de JVM: el binario nativo arranca en <1 s y el RSS
# queda muy por debajo de los 512 MB del free tier. Límite de heap por las dudas.
ENTRYPOINT ["./truco", "-XX:MaximumHeapSizePercent=70"]
