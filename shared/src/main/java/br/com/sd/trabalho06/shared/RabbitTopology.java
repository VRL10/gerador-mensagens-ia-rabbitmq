package br.com.sd.trabalho06.shared;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

public final class RabbitTopology {
    public static final String EXCHANGE_NAME = "trabalho06.topic";
    public static final String PLATE_QUEUE = "trabalho06.plates";
    public static final String SIGN_QUEUE = "trabalho06.signs";
    public static final String PLATE_ROUTING_KEY = "plate";
    public static final String SIGN_ROUTING_KEY = "sign";

    private RabbitTopology() {
    }

    public static ConnectionFactory createConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(env("RABBITMQ_HOST", "rabbitmq"));
        factory.setPort(Integer.parseInt(env("RABBITMQ_PORT", "5672")));
        factory.setUsername(env("RABBITMQ_USER", "guest"));
        factory.setPassword(env("RABBITMQ_PASSWORD", "guest"));
        factory.setVirtualHost(env("RABBITMQ_VHOST", "/"));
        return factory;
    }

    public static void declareTopology(Channel channel) throws IOException {
        channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
        channel.queueDeclare(PLATE_QUEUE, true, false, false, null);
        channel.queueDeclare(SIGN_QUEUE, true, false, false, null);
        channel.queueBind(PLATE_QUEUE, EXCHANGE_NAME, PLATE_ROUTING_KEY);
        channel.queueBind(SIGN_QUEUE, EXCHANGE_NAME, SIGN_ROUTING_KEY);
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}