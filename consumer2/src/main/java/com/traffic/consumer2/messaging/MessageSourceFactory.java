package com.traffic.consumer2.messaging;

import com.traffic.consumer2.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fábrica que instancia a implementação correta de {@link MessageSource}
 * com base na variável de ambiente USE_MOCK.
 *
 * Ponto único de decisão — nenhuma outra classe precisa saber qual
 * implementação está em uso.
 */
public final class MessageSourceFactory {

    private static final Logger log = LoggerFactory.getLogger(MessageSourceFactory.class);

    private MessageSourceFactory() {}

    public static MessageSource create() {
        AppConfig cfg = AppConfig.get();

        if (cfg.useMock) {
            log.info("[MessageSourceFactory] Modo MOCK ativado (USE_MOCK=true)");
            return new MockMessageSource();
        }

        log.info("[MessageSourceFactory] Modo REAL ativado — conectando ao RabbitMQ em {}:{}",
                cfg.rabbitHost, cfg.rabbitPort);
        try {
            return new RabbitMQMessageSource();
        } catch (Exception e) {
            log.error("[MessageSourceFactory] Falha ao conectar ao RabbitMQ: {}. " +
                      "Verifique as variáveis RABBITMQ_* ou ative USE_MOCK=true.", e.getMessage());
            throw new RuntimeException("Não foi possível criar a fonte de mensagens", e);
        }
    }
}
