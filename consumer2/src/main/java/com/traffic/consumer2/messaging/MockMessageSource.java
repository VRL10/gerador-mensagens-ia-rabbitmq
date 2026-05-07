package com.traffic.consumer2.messaging;

import com.traffic.consumer2.config.AppConfig;
import com.traffic.consumer2.model.TrafficSignMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fonte de mensagens simuladas — não requer nenhuma infraestrutura externa.
 *
 * Gera mensagens com intervalo configurável (MOCK_RATE_MS) simulando a
 * chegada de imagens de sinais de trânsito com metadados variados.
 *
 * Os metadados descrevem características visuais do sinal (forma, cor,
 * presença de número/seta) que o {@link com.traffic.consumer2.ai.SignClassifier}
 * usa como features de entrada para o modelo Smile.
 */
public class MockMessageSource implements MessageSource {

    private static final Logger log = LoggerFactory.getLogger(MockMessageSource.class);

    // Cenários mock: cada entrada simula um tipo real de sinal
    private static final List<String> MOCK_SCENARIOS = List.of(
        // shape=octagonal, color=red, hasNumber=0, hasArrow=0  → STOP
        "{\"shape\":2,\"color\":0,\"hasNumber\":0,\"hasArrow\":0,\"brightness\":0.75}",
        // shape=circular, color=red, hasNumber=1, hasArrow=0   → SPEED_LIMIT
        "{\"shape\":0,\"color\":0,\"hasNumber\":1,\"hasArrow\":0,\"brightness\":0.80}",
        // shape=circular, color=red, hasNumber=0, hasArrow=1   → NO_TURN
        "{\"shape\":0,\"color\":0,\"hasNumber\":0,\"hasArrow\":1,\"brightness\":0.70}",
        // shape=triangular, color=red, hasNumber=0, hasArrow=0 → YIELD
        "{\"shape\":1,\"color\":0,\"hasNumber\":0,\"hasArrow\":0,\"brightness\":0.65}",
        // shape=circular, color=red, hasNumber=0, hasArrow=0   → NO_ENTRY
        "{\"shape\":0,\"color\":0,\"hasNumber\":0,\"hasArrow\":0,\"brightness\":0.90}",
        // shape=rectangular, color=blue, hasNumber=0, hasArrow=0 → PEDESTRIAN
        "{\"shape\":3,\"color\":1,\"hasNumber\":0,\"hasArrow\":0,\"brightness\":0.60}",
        // shape=triangular, color=yellow, hasNumber=0, hasArrow=0 → WARNING_AHEAD
        "{\"shape\":1,\"color\":2,\"hasNumber\":0,\"hasArrow\":0,\"brightness\":0.55}",
        // shape=circular, color=blue, hasNumber=0, hasArrow=1  → NO_TURN_LEFT
        "{\"shape\":0,\"color\":1,\"hasNumber\":0,\"hasArrow\":1,\"brightness\":0.72}",
        // shape=circular, color=red, hasNumber=1, hasArrow=1   → NO_TURN_RIGHT (speed+arrow)
        "{\"shape\":0,\"color\":0,\"hasNumber\":1,\"hasArrow\":1,\"brightness\":0.68}"
    );

    private final long        rateMs;
    private final Random      random    = new Random();
    private final AtomicLong  counter   = new AtomicLong(0);
    private volatile boolean  running   = true;

    public MockMessageSource() {
        this.rateMs = AppConfig.get().mockRateMs;
        log.info("[MockMessageSource] Inicializado — gerando 1 mensagem a cada {}ms", rateMs);
    }

    @Override
    public TrafficSignMessage nextMessage() throws InterruptedException {
        Thread.sleep(rateMs);
        if (!running) return null;

        String scenario = MOCK_SCENARIOS.get(random.nextInt(MOCK_SCENARIOS.size()));
        TrafficSignMessage msg = TrafficSignMessage.mock(scenario);

        long count = counter.incrementAndGet();
        log.debug("[MockMessageSource] Mensagem #{} gerada: {}", count, scenario);
        return msg;
    }

    @Override
    public void acknowledge(TrafficSignMessage message) {
        // Mock: nada a confirmar
    }

    @Override
    public void reject(TrafficSignMessage message) {
        log.warn("[MockMessageSource] Mensagem rejeitada (mock): {}", message.getMessageId());
    }

    @Override
    public String describe() {
        return String.format("MockMessageSource(rate=%dms, total=%d)", rateMs, counter.get());
    }

    @Override
    public void close() {
        running = false;
        log.info("[MockMessageSource] Encerrado após {} mensagens", counter.get());
    }
}
