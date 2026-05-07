# ── Estágio 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache de dependências (só rebaixa quando pom.xml muda)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Compila e gera o fat-jar
COPY src ./src
RUN mvn package -DskipTests -q

# ── Estágio 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia apenas o fat-jar
COPY --from=builder /build/target/consumer2-sign-classifier-1.0.0.jar app.jar

# ── Variáveis de ambiente com defaults ────────────────────────────────────────
#   Altere via docker-compose ou `docker run -e` para mudar o comportamento
ENV USE_MOCK=true
ENV MOCK_RATE_MS=1500

# Para modo real, configure abaixo:
ENV RABBITMQ_HOST=rabbitmq
ENV RABBITMQ_PORT=5672
ENV RABBITMQ_USER=guest
ENV RABBITMQ_PASS=guest
ENV RABBITMQ_VHOST=/
ENV EXCHANGE_NAME=traffic.exchange
ENV QUEUE_NAME=sign.queue
ENV ROUTING_KEY=sign

# ── Healthcheck básico ────────────────────────────────────────────────────────
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
  CMD pgrep -f "app.jar" || exit 1

# ── Execução ──────────────────────────────────────────────────────────────────
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
