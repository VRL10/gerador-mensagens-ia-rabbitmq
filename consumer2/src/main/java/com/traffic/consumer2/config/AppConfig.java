package com.traffic.consumer2.config;

/**
 * Centraliza todas as configurações do serviço.
 *
 * Variáveis de ambiente disponíveis:
 *
 *   USE_MOCK         true|false   (default: true)  – usa gerador interno em vez de RabbitMQ
 *   MOCK_RATE_MS     inteiro      (default: 1500)   – intervalo entre mensagens mock em ms
 *
 *   RABBITMQ_HOST    string       (default: rabbitmq)
 *   RABBITMQ_PORT    inteiro      (default: 5672)
 *   RABBITMQ_USER    string       (default: guest)
 *   RABBITMQ_PASS    string       (default: guest)
 *   RABBITMQ_VHOST   string       (default: /)
 *
 *   EXCHANGE_NAME    string       (default: traffic.exchange)
 *   QUEUE_NAME       string       (default: sign.queue)
 *   ROUTING_KEY      string       (default: sign)
 *
 * Para conectar ao sistema real basta setar USE_MOCK=false
 * e as variáveis RABBITMQ_* no docker-compose / ambiente.
 */
public final class AppConfig {

    // ── Modo de execução ────────────────────────────────────────────────────
    public final boolean useMock;
    public final long    mockRateMs;

    // ── RabbitMQ ────────────────────────────────────────────────────────────
    public final String rabbitHost;
    public final int    rabbitPort;
    public final String rabbitUser;
    public final String rabbitPass;
    public final String rabbitVhost;

    // ── Topologia de mensageria ─────────────────────────────────────────────
    public final String exchangeName;
    public final String queueName;
    public final String routingKey;

    private AppConfig() {
        useMock      = Boolean.parseBoolean(env("USE_MOCK",        "true"));
        mockRateMs   = Long.parseLong      (env("MOCK_RATE_MS",    "1500"));

        rabbitHost   = env("RABBITMQ_HOST",  "rabbitmq");
        rabbitPort   = Integer.parseInt(env("RABBITMQ_PORT",  "5672"));
        rabbitUser   = env("RABBITMQ_USER",  "guest");
        rabbitPass   = env("RABBITMQ_PASS",  "guest");
        rabbitVhost  = env("RABBITMQ_VHOST", "/");

        exchangeName = env("EXCHANGE_NAME",  "traffic.exchange");
        queueName    = env("QUEUE_NAME",     "sign.queue");
        routingKey   = env("ROUTING_KEY",    "sign");
    }

    // ── Singleton ───────────────────────────────────────────────────────────
    private static final AppConfig INSTANCE = new AppConfig();
    public static AppConfig get() { return INSTANCE; }

    // ── Helpers ─────────────────────────────────────────────────────────────
    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }

    @Override
    public String toString() {
        return String.format(
            "AppConfig{useMock=%b, mockRateMs=%d, rabbitHost='%s', rabbitPort=%d, " +
            "exchange='%s', queue='%s', routingKey='%s'}",
            useMock, mockRateMs, rabbitHost, rabbitPort,
            exchangeName, queueName, routingKey
        );
    }
}
