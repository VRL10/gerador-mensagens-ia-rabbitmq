package br.com.sd.trabalho06.generator;

import br.com.sd.trabalho06.shared.ImageCodec;
import br.com.sd.trabalho06.shared.MessageJson;
import br.com.sd.trabalho06.shared.MessageKind;
import br.com.sd.trabalho06.shared.PlateMessage;
import br.com.sd.trabalho06.shared.RabbitTopology;
import br.com.sd.trabalho06.shared.SignMessage;
import br.com.sd.trabalho06.shared.SignType;
import br.com.sd.trabalho06.shared.SyntheticImageFactory;
import br.com.sd.trabalho06.shared.VehicleType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeoutException;

public final class GeneratorApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorApp.class);

    private GeneratorApp() {
    }

    public static void main(String[] args) throws Exception {
        Random random = new Random();
        AtomicLong sequence = new AtomicLong();
        AtomicBoolean sendPlateNext = new AtomicBoolean(true);

        try (Connection connection = RabbitTopology.createConnectionFactory().newConnection();
             Channel channel = connection.createChannel()) {
            RabbitTopology.declareTopology(channel);
            channel.confirmSelect();

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(executor)));

            executor.scheduleAtFixedRate(() -> {
                try {
                    long current = sequence.incrementAndGet();
                    if (sendPlateNext.getAndSet(!sendPlateNext.get())) {
                        publishPlate(channel, current, random);
                    } else {
                        publishSign(channel, current, random);
                    }
                } catch (Exception exception) {
                    LOGGER.error("Falha ao publicar mensagem", exception);
                }
            }, 0L, 100L, TimeUnit.MILLISECONDS);

            LOGGER.info("Gerador ativo. Publicando aproximadamente 10 mensagens por segundo.");
            new CountDownLatch(1).await();
        }
    }

    private static void publishPlate(Channel channel, long sequence, Random random) throws IOException, InterruptedException, TimeoutException {
        VehicleType vehicleType = SyntheticImageFactory.randomVehicleType(random);
        String plateText = SyntheticImageFactory.createRandomPlateText(random);
        BufferedImage image = SyntheticImageFactory.createPlateImage(plateText, vehicleType);
        PlateMessage message = new PlateMessage(
                UUID.randomUUID(),
                MessageKind.PLATE,
                plateText,
                vehicleType,
                System.currentTimeMillis(),
                ImageCodec.encodeBase64Png(image),
                image.getWidth(),
                image.getHeight()
        );
        publish(channel, RabbitTopology.PLATE_ROUTING_KEY, MessageJson.toJson(message));
        LOGGER.info("{} -> plate {} ({})", sequence, plateText, vehicleType.displayName());
        Thread.sleep(5L);
    }

    private static void publishSign(Channel channel, long sequence, Random random) throws IOException, InterruptedException, TimeoutException {
        SignType signType = SyntheticImageFactory.randomSignType(random);
        BufferedImage image = SyntheticImageFactory.createSignImage(signType);
        SignMessage message = new SignMessage(
                UUID.randomUUID(),
                MessageKind.SIGN,
                signType,
                System.currentTimeMillis(),
                ImageCodec.encodeBase64Png(image),
                image.getWidth(),
                image.getHeight()
        );
        publish(channel, RabbitTopology.SIGN_ROUTING_KEY, MessageJson.toJson(message));
        LOGGER.info("{} -> sign {}", sequence, signType.displayName());
        Thread.sleep(5L);
    }

    private static void publish(Channel channel, String routingKey, String payload) throws IOException, InterruptedException, TimeoutException {
        channel.basicPublish(RabbitTopology.EXCHANGE_NAME, routingKey, null, payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        channel.waitForConfirmsOrDie(5_000L);
    }

    private static void shutdown(ScheduledExecutorService executor) {
        executor.shutdownNow();
    }
}