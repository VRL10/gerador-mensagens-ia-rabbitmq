package com.traffic.consumer2.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.consumer2.model.TrafficSignMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extrai um vetor de features numéricas de uma {@link TrafficSignMessage}.
 *
 * Features extraídas (índices do vetor):
 *   [0] shape      – 0=circular, 1=triangular, 2=octagonal, 3=rectangular
 *   [1] color      – 0=red,      1=blue,       2=yellow,    3=white
 *   [2] hasNumber  – 0=não,      1=sim
 *   [3] hasArrow   – 0=não,      1=sim
 *   [4] brightness – 0.0–1.0 (intensidade média dos pixels)
 *
 * Em modo real, esses valores devem vir de um pipeline de visão computacional
 * que processa o campo imageDataBase64. Por enquanto, são lidos diretamente
 * do campo metadata (JSON) que o mock já preenche no formato correto.
 *
 * Para integrar com processamento real de imagem, basta substituir o método
 * {@link #extractFromImage(String)} mantendo a assinatura do vetor.
 */
public class FeatureExtractor {

    private static final Logger log = LoggerFactory.getLogger(FeatureExtractor.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final int FEATURE_COUNT = 5;

    private FeatureExtractor() {}

    /**
     * Extrai features da mensagem.
     *
     * @return double[] com {@value FEATURE_COUNT} features, nunca null
     */
    public static double[] extract(TrafficSignMessage msg) {
        // 1. Tenta extrair da imagem real (stub — implementar quando disponível)
        if (msg.getImageDataBase64() != null && !msg.getImageDataBase64().isBlank()) {
            return extractFromImage(msg.getImageDataBase64());
        }

        // 2. Tenta extrair do metadata JSON (gerado pelo mock ou pelo produtor)
        if (msg.getMetadata() != null && !msg.getMetadata().isBlank()) {
            return extractFromMetadata(msg.getMetadata());
        }

        // 3. Fallback: vetor zerado (não deve ocorrer em operação normal)
        log.warn("[FeatureExtractor] Mensagem {} sem dados — usando vetor default", msg.getMessageId());
        return new double[FEATURE_COUNT];
    }

    // ── Extração de metadados JSON ──────────────────────────────────────────
    private static double[] extractFromMetadata(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            return new double[]{
                node.path("shape")     .asDouble(0),
                node.path("color")     .asDouble(0),
                node.path("hasNumber") .asDouble(0),
                node.path("hasArrow")  .asDouble(0),
                node.path("brightness").asDouble(0.5)
            };
        } catch (Exception e) {
            log.error("[FeatureExtractor] Falha ao parsear metadata JSON: {}", e.getMessage());
            return new double[FEATURE_COUNT];
        }
    }

    /**
     * Extração de features a partir da imagem Base64.
     *
     * TODO: Implementar pipeline real de visão computacional:
     *   1. Decodificar Base64 → BufferedImage
     *   2. Detectar forma dominante (Hough Circles, corner detection)
     *   3. Analisar histograma de cores (HSV)
     *   4. OCR simples para detectar números (Tesseract ou Smile NLP)
     *   5. Detecção de seta via template matching
     *   6. Calcular brilho médio
     *
     * @param base64Image imagem codificada em Base64
     * @return vetor de features
     */
    private static double[] extractFromImage(String base64Image) {
        // Stub — retorna features neutras até implementação real
        log.debug("[FeatureExtractor] Extração de imagem ainda não implementada — usando features neutras");
        return new double[]{0, 0, 0, 0, 0.5};
    }
}
