package com.traffic.consumer2.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.traffic.consumer2.config.AppConfig;
import com.traffic.consumer2.model.TrafficSignMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fonte de mensagens real consumindo do RabbitMQ.
 *
 * Topologia esperada (criada pelo Gerador de Mensagens ou aqui se não existir):
 *   Exchange : traffic.exchange  (type=topic, durable)
 *   Queue    : sign.queue        (durable)
 *   Binding  : sign.# → sign.queue
 *
 * Para ativar este modo basta setar USE_MOCK=false nas variáveis de ambiente.
 *
 * O deliveryTag de cada mensagem é preservado no campo metadata para que
 * acknowledge() e reject() possam fazer basicAck/basicNack corretamente.
 */
public class RabbitMQMessageSource implements MessageSource {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQMessageSource.class);

    private static final int PREFETCH_COUNT = 10;   // quantas msgs buscar por vez
    private static final int POLL_TIMEOUT_MS = 500; // timeout no poll da fila interna

    private final AppConfig     cfg;
    private final ObjectMapper  mapper = new ObjectMapper();

    private Connection connection;
    private Channel    channel;

    /** Fila interna que desacopla o callback do RabbitMQ do loop de processamento. */
    private final BlockingQueue<RabbitDelivery> inbox = new LinkedBlockingQueue<>(500);

    /** Mapa deliveryTag → mensagem para o ACK posterior. */
    private final Map<String, Long> pendingTags = new HashMap<>();

    private volatile boolean running = true;

    public RabbitMQMessageSource() throws IOException, TimeoutException {
        this.cfg = AppConfig.get();
        connect();
    }

    // ── Conexão ─────────────────────────────────────────────────────────────
    private void connect() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(cfg.rabbitHost);
        factory.setPort(cfg.rabbitPort);
        factory.setUsername(cfg.rabbitUser);
        factory.setPassword(cfg.rabbitPass);
        factory.setVirtualHost(cfg.rabbitVhost);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5_000);

        connection = factory.newConnection("consumer2-sign-classifier");
        channel    = connection.createChannel();

        // Declara exchange, fila e binding (idempotente — seguro chamar várias vezes)
        channel.exchangeDeclare(cfg.exchangeName, "topic", true);
        channel.queueDeclare(cfg.queueName, true, false, false, null);
        channel.queueBind(cfg.queueName, cfg.exchangeName, cfg.routingKey + ".#");

        // Backpressure: não buscar mais que PREFETCH_COUNT mensagens de uma vez
        channel.basicQos(PREFETCH_COUNT);

        // Registra o consumer assíncrono
        channel.basicConsume(cfg.queueName, false /*autoAck=false*/, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties props,
                                       byte[] body) {
                inbox.offer(new RabbitDelivery(envelope.getDeliveryTag(), body));
            }
        });

        log.info("[RabbitMQMessageSource] Conectado a {}:{} — escutando '{}'",
                cfg.rabbitHost, cfg.rabbitPort, cfg.queueName);
    }

    // ── MessageSource ────────────────────────────────────────────────────────
    @Override
    public TrafficSignMessage nextMessage() throws InterruptedException {
        while (running) {
            RabbitDelivery delivery = inbox.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (delivery == null) continue;

            try {
                TrafficSignMessage msg = deserialize(delivery.body);
                // Guarda o deliveryTag para o ACK
                pendingTags.put(msg.getMessageId(), delivery.deliveryTag);
                return msg;
            } catch (Exception e) {
                log.error("[RabbitMQMessageSource] Falha ao deserializar mensagem, descartando", e);
                silentNack(delivery.deliveryTag);
            }
        }
        return null;
    }

    @Override
    public void acknowledge(TrafficSignMessage message) {
        Long tag = pendingTags.remove(message.getMessageId());
        if (tag == null) return;
        try {
            channel.basicAck(tag, false);
        } catch (IOException e) {
            log.warn("[RabbitMQMessageSource] Falha no ACK: {}", e.getMessage());
        }
    }

    @Override
    public void reject(TrafficSignMessage message) {
        Long tag = pendingTags.remove(message.getMessageId());
        if (tag == null) return;
        silentNack(tag);
    }

    @Override
    public String describe() {
        return String.format("RabbitMQMessageSource(host=%s, queue=%s)",
                cfg.rabbitHost, cfg.queueName);
    }

    @Override
    public void close() {
        running = false;
        try { if (channel    != null && channel.isOpen())    channel.close();    } catch (Exception ignored) {}
        try { if (connection != null && connection.isOpen()) connection.close(); } catch (Exception ignored) {}
        log.info("[RabbitMQMessageSource] Conexão encerrada.");
    }

    // ── Internos ─────────────────────────────────────────────────────────────
    private TrafficSignMessage deserialize(byte[] body) throws Exception {
        String json = new String(body);
        TrafficSignMessage msg;
        try {
            msg = mapper.readValue(json, TrafficSignMessage.class);
        } catch (Exception e) {
            // Fallback: se a mensagem for JSON simples de features, encapsula como metadata
            msg = new TrafficSignMessage();
            msg.setMessageId(java.util.UUID.randomUUID().toString());
            msg.setTimestamp(Instant.now());
            msg.setRoutingKey("sign");
            msg.setMetadata(json);
        }
        return msg;
    }

    private void silentNack(long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.warn("[RabbitMQMessageSource] Falha no NACK silencioso", e);
        }
    }

    // ── Record interno ────────────────────────────────────────────────────────
    private record RabbitDelivery(long deliveryTag, byte[] body) {}
}
