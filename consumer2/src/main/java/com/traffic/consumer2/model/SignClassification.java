package com.traffic.consumer2.model;

import java.time.Instant;

/**
 * Resultado da classificação de um sinal de trânsito.
 */
public class SignClassification {

    /** Categorias que o modelo pode reconhecer. */
    public enum Label {
        STOP            ("Pare"),
        SPEED_LIMIT     ("Velocidade Máxima"),
        NO_TURN_LEFT    ("Proibido Virar à Esquerda"),
        NO_TURN_RIGHT   ("Proibido Virar à Direita"),
        NO_ENTRY        ("Proibido Entrar"),
        YIELD           ("Ceda a Passagem"),
        WARNING_AHEAD   ("Advertência Geral"),
        PEDESTRIAN      ("Passagem de Pedestres"),
        UNKNOWN         ("Desconhecido");

        private final String description;
        Label(String d) { this.description = d; }
        public String getDescription() { return description; }
    }

    private String    messageId;
    private Label     label;
    private double    confidence;      // 0.0 – 1.0
    private Instant   processedAt;
    private long      processingMs;    // tempo de inferência em ms

    // ── Builder estático ───────────────────────────────────────────────────
    public static SignClassification of(String messageId,
                                        Label label,
                                        double confidence,
                                        long processingMs) {
        SignClassification r = new SignClassification();
        r.messageId    = messageId;
        r.label        = label;
        r.confidence   = confidence;
        r.processedAt  = Instant.now();
        r.processingMs = processingMs;
        return r;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String  getMessageId()    { return messageId; }
    public Label   getLabel()        { return label; }
    public double  getConfidence()   { return confidence; }
    public Instant getProcessedAt()  { return processedAt; }
    public long    getProcessingMs() { return processingMs; }

    @Override
    public String toString() {
        return String.format(
            "SignClassification{msgId='%s', label=%s (%s), confidence=%.1f%%, time=%dms}",
            messageId, label.name(), label.getDescription(),
            confidence * 100, processingMs
        );
    }
}
