package br.com.sd.trabalho06.consumer.sign;

import br.com.sd.trabalho06.shared.ImageCodec;
import br.com.sd.trabalho06.shared.ImageFeatures;
import br.com.sd.trabalho06.shared.MessageJson;
import br.com.sd.trabalho06.shared.RabbitTopology;
import br.com.sd.trabalho06.shared.SignMessage;
import br.com.sd.trabalho06.shared.SignType;
import br.com.sd.trabalho06.shared.SyntheticImageFactory;
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

public final class SignConsumerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignConsumerApp.class);

    private SignConsumerApp() {
    }

    public static void main(String[] args) throws Exception {
        KNN<double[]> classifier = trainClassifier();

        try (Connection connection = RabbitTopology.createConnectionFactory().newConnection();
             Channel channel = connection.createChannel()) {
            RabbitTopology.declareTopology(channel);
            channel.basicQos(1);

            CountDownLatch keepAlive = new CountDownLatch(1);
            channel.basicConsume(RabbitTopology.SIGN_QUEUE, false, (consumerTag, delivery) -> handleDelivery(channel, classifier, delivery), consumerTag -> {});

            LOGGER.info("Consumidor de sinais ativo.");
            keepAlive.await();
        }
    }

    private static void handleDelivery(Channel channel, KNN<double[]> classifier, Delivery delivery) {
        try {
            SignMessage message = MessageJson.fromJson(new String(delivery.getBody(), StandardCharsets.UTF_8), SignMessage.class);
            BufferedImage image = ImageCodec.decodeBase64Png(message.imageBase64());
            SignType predictedType = SignType.values()[classifier.predict(ImageFeatures.signFeatures(image))];

            LOGGER.info("sinal={} previsto={} esperado={} id={}", message.signType().displayName(), predictedType.displayName(), message.signType().displayName(), message.id());
            Thread.sleep(Duration.ofMillis(1200L).toMillis());
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } catch (Exception e) {
            LOGGER.error("Erro ao processar mensagem de sinal", e);
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

        for (SignType signType : SignType.values()) {
            for (int index = 0; index < 25; index++) {
                BufferedImage image = SyntheticImageFactory.createSignImage(signType);
                features.add(ImageFeatures.signFeatures(image));
                labels.add(signType.ordinal());
            }
        }

        double[][] x = features.toArray(double[][]::new);
        int[] y = labels.stream().mapToInt(Integer::intValue).toArray();
        return KNN.fit(x, y, 3);
    }
}