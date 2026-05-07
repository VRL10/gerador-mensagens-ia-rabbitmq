package br.com.sd.trabalho06.consumer.plate;

import br.com.sd.trabalho06.shared.ImageCodec;
import br.com.sd.trabalho06.shared.ImageFeatures;
import br.com.sd.trabalho06.shared.MessageJson;
import br.com.sd.trabalho06.shared.PlateMessage;
import br.com.sd.trabalho06.shared.RabbitTopology;
import br.com.sd.trabalho06.shared.TemplateOcr;
import br.com.sd.trabalho06.shared.VehicleType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.classification.KNN;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class PlateConsumerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlateConsumerApp.class);

    private PlateConsumerApp() {
    }

    public static void main(String[] args) throws Exception {
        KNN<double[]> classifier = trainClassifier();

        try (Connection connection = RabbitTopology.createConnectionFactory().newConnection();
             Channel channel = connection.createChannel()) {
            RabbitTopology.declareTopology(channel);
            channel.basicQos(1);

            CountDownLatch keepAlive = new CountDownLatch(1);
            channel.basicConsume(RabbitTopology.PLATE_QUEUE, false, (consumerTag, delivery) -> handleDelivery(channel, classifier, delivery), consumerTag -> {});

            LOGGER.info("Consumidor de placas ativo.");
            keepAlive.await();
        }
    }

    private static void handleDelivery(Channel channel, KNN<double[]> classifier, Delivery delivery) {
        try {
            PlateMessage message = MessageJson.fromJson(new String(delivery.getBody(), StandardCharsets.UTF_8), PlateMessage.class);
            BufferedImage image = ImageCodec.decodeBase64Png(message.imageBase64());
            String recognizedPlate = TemplateOcr.recognizeSevenCharacterPlate(image);
            VehicleType predictedType = VehicleType.values()[classifier.predict(ImageFeatures.vehicleFeatures(image))];

            LOGGER.info("placa={} prevista={} esperada={} id={}", recognizedPlate, predictedType.displayName(), message.vehicleType().displayName(), message.id());
            Thread.sleep(Duration.ofMillis(1200L).toMillis());
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } catch (Exception e) {
            LOGGER.error("Erro ao processar mensagem de placa", e);
            try {
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            } catch (Exception ex) {
                LOGGER.error("Falha ao reenfileirar mensagem", ex);
            }
        }
    }

    private static KNN<double[]> trainClassifier() {
        List<double[]> features = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        for (VehicleType vehicleType : VehicleType.values()) {
            for (int index = 0; index < 25; index++) {
                String plateText = String.format("%s%02d%02d", vehicleType.name().substring(0, 1), index % 100, (index * 7) % 100);
                BufferedImage image = br.com.sd.trabalho06.shared.SyntheticImageFactory.createPlateImage(plateText, vehicleType);
                features.add(ImageFeatures.vehicleFeatures(image));
                labels.add(vehicleType.ordinal());
            }
        }

        double[][] x = features.toArray(double[][]::new);
        int[] y = labels.stream().mapToInt(Integer::intValue).toArray();
        return KNN.fit(x, y, 3);
    }
}