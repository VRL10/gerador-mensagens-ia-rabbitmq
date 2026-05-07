package com.traffic.consumer2;

import com.traffic.consumer2.ai.SignClassifier;
import com.traffic.consumer2.config.AppConfig;
import com.traffic.consumer2.messaging.MessageSource;
import com.traffic.consumer2.messaging.MessageSourceFactory;
import com.traffic.consumer2.model.SignClassification;
import com.traffic.consumer2.model.TrafficSignMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║         Consumer 2 – Traffic Sign Classifier                ║
 * ║                                                              ║
 * ║  Consome mensagens do RabbitMQ (routing key: sign) e        ║
 * ║  classifica o tipo de sinal usando Smile RandomForest.      ║
 * ║                                                              ║
 * ║  Variáveis de ambiente principais:                          ║
 * ║    USE_MOCK=true      → usa gerador interno (padrão)        ║
 * ║    USE_MOCK=false     → conecta ao RabbitMQ real            ║
 * ║    MOCK_RATE_MS=1500  → intervalo entre msgs mock (ms)      ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    // Simula processamento mais lento que a geração (requisito do trabalho)
    private static final long PROCESSING_DELAY_MS = 800;

    public static void main(String[] args) {
        AppConfig cfg = AppConfig.get();
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║  Consumer 2 – Traffic Sign Classifier iniciando  ║");
        log.info("╚══════════════════════════════════════════════════╝");
        log.info("Config: {}", cfg);

        // ── Inicializa o modelo de IA (treino sintético) ─────────────────────
        log.info("Inicializando modelo Smile...");
        SignClassifier classifier = new SignClassifier();

        // ── Registra shutdown hook ───────────────────────────────────────────
        AtomicLong processed = new AtomicLong(0);
        AtomicLong errors    = new AtomicLong(0);
        final MessageSource[] sourceRef = {null};

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("──────────────────────────────────────────────────");
            log.info("Encerrando Consumer 2...");
            log.info("  Mensagens processadas : {}", processed.get());
            log.info("  Erros                 : {}", errors.get());
            if (sourceRef[0] != null) {
                try { sourceRef[0].close(); } catch (Exception ignored) {}
            }
            log.info("──────────────────────────────────────────────────");
        }, "shutdown-hook"));

        // ── Loop principal ──────────────────────────────────────────────────
        try (MessageSource source = MessageSourceFactory.create()) {
            sourceRef[0] = source;
            log.info("Fonte de mensagens ativa: {}", source.describe());
            log.info("Aguardando mensagens...\n");

            while (!Thread.currentThread().isInterrupted()) {
                TrafficSignMessage message = source.nextMessage();
                if (message == null) break; // fonte encerrada

                try {
                    // Simula processamento mais lento que o produtor
                    Thread.sleep(PROCESSING_DELAY_MS);

                    SignClassification result = classifier.classify(message);
                    source.acknowledge(message);

                    long count = processed.incrementAndGet();
                    logResult(count, message, result);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    errors.incrementAndGet();
                    log.error("Erro ao processar mensagem {}: {}",
                            message.getMessageId(), e.getMessage(), e);
                    source.reject(message);
                }
            }
        } catch (Exception e) {
            log.error("Erro fatal no Consumer 2: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void logResult(long count, TrafficSignMessage msg, SignClassification result) {
        log.info("─── Mensagem #{} ─────────────────────────────────────", count);
        log.info("  ID          : {}", msg.getMessageId());
        log.info("  Recebida em : {}", msg.getTimestamp());
        log.info("  Metadata    : {}", msg.getMetadata());
        log.info("  ┌─ Classificação ─────────────────────────────────");
        log.info("  │  Sinal       : {} ({})",
                result.getLabel().name(), result.getLabel().getDescription());
        log.info("  │  Confiança   : {:.1f}%", result.getConfidence() * 100);
        log.info("  │  Tempo IA    : {}ms", result.getProcessingMs());
        log.info("  └─────────────────────────────────────────────────");
    }
}
