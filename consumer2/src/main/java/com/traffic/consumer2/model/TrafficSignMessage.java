package com.traffic.consumer2.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa uma mensagem recebida do broker (ou do mock).
 *
 * Campos que o Gerador de Mensagens deve preencher ao publicar:
 *   - messageId  : UUID único da mensagem
 *   - timestamp  : instante de criação no produtor
 *   - routingKey : deve ser "sign" para chegar neste consumidor
 *   - imageData  : bytes da imagem codificados em Base64 (real) ou null (mock)
 *   - metadata   : qualquer metadado extra em formato livre
 *
 * Em modo mock os campos imageData/metadata são preenchidos internamente.
 */
public class TrafficSignMessage {

    private String  messageId;
    private Instant timestamp;
    private String  routingKey;
    private String  imageDataBase64;   // null em modo mock
    private String  metadata;

    // ── Construtor de conveniência para o mock ──────────────────────────────
    public static TrafficSignMessage mock(String metadata) {
        TrafficSignMessage m = new TrafficSignMessage();
        m.messageId       = UUID.randomUUID().toString();
        m.timestamp       = Instant.now();
        m.routingKey      = "sign";
        m.imageDataBase64 = null;
        m.metadata        = metadata;
        return m;
    }

    // ── Getters / Setters ───────────────────────────────────────────────────
    public String  getMessageId()         { return messageId; }
    public void    setMessageId(String v) { this.messageId = v; }

    public Instant getTimestamp()           { return timestamp; }
    public void    setTimestamp(Instant v)  { this.timestamp = v; }

    public String  getRoutingKey()          { return routingKey; }
    public void    setRoutingKey(String v)  { this.routingKey = v; }

    public String  getImageDataBase64()     { return imageDataBase64; }
    public void    setImageDataBase64(String v) { this.imageDataBase64 = v; }

    public String  getMetadata()            { return metadata; }
    public void    setMetadata(String v)    { this.metadata = v; }

    @Override
    public String toString() {
        return String.format("TrafficSignMessage{id='%s', ts=%s, key='%s', meta='%s'}",
                messageId, timestamp, routingKey, metadata);
    }
}
