package com.traffic.consumer2.ai;

import com.traffic.consumer2.model.SignClassification;
import com.traffic.consumer2.model.SignClassification.Label;
import com.traffic.consumer2.model.TrafficSignMessage;
import com.traffic.consumer2.util.FeatureExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;

/**
 * Classificador de sinais de trânsito usando Smile RandomForest.
 *
 * ── Modelo ──────────────────────────────────────────────────────────────────
 * Features (5 entradas):
 *   shape      – {0=circular, 1=triangular, 2=octagonal, 3=rectangular}
 *   color      – {0=red, 1=blue, 2=yellow, 3=white}
 *   hasNumber  – {0, 1}
 *   hasArrow   – {0, 1}
 *   brightness – [0.0, 1.0]
 *
 * Labels (9 classes):
 *   0=STOP, 1=SPEED_LIMIT, 2=NO_TURN_LEFT, 3=NO_TURN_RIGHT,
 *   4=NO_ENTRY, 5=YIELD, 6=WARNING_AHEAD, 7=PEDESTRIAN, 8=UNKNOWN
 *
 * ── Treino ──────────────────────────────────────────────────────────────────
 * O modelo é treinado na inicialização com dados sintéticos que codificam
 * regras do Código Brasileiro de Trânsito.
 *
 * Substituição futura: carregar pesos de um arquivo .smile (serializado)
 * ou treinar com dataset real de sinais (ex.: GTSRB).
 */
public class SignClassifier {

    private static final Logger log = LoggerFactory.getLogger(SignClassifier.class);

    private final RandomForest model;
    private final Label[]      labelIndex = Label.values();

    // ── Dados de treino sintéticos ──────────────────────────────────────────
    // Cada linha: [shape, color, hasNumber, hasArrow, brightness, label]
    // Gerados manualmente para refletir características reais dos sinais
    private static final double[][] TRAINING_FEATURES = {
        // STOP (2) – octagonal, red, sem número, sem seta
        {2, 0, 0, 0, 0.75}, {2, 0, 0, 0, 0.80}, {2, 0, 0, 0, 0.70},
        {2, 0, 0, 0, 0.65}, {2, 0, 0, 0, 0.85},

        // SPEED_LIMIT (1) – circular, red, com número, sem seta
        {0, 0, 1, 0, 0.80}, {0, 0, 1, 0, 0.75}, {0, 0, 1, 0, 0.85},
        {0, 0, 1, 0, 0.90}, {0, 0, 1, 0, 0.70},

        // NO_TURN_LEFT (2) – circular, red, sem número, com seta
        {0, 0, 0, 1, 0.70}, {0, 0, 0, 1, 0.75}, {0, 0, 0, 1, 0.65},

        // NO_TURN_RIGHT (3) – circular, red, com número, com seta (dupla proibição)
        {0, 0, 1, 1, 0.68}, {0, 0, 1, 1, 0.72}, {0, 0, 1, 1, 0.60},

        // NO_ENTRY (4) – circular, red, sem número, sem seta, alto brilho
        {0, 0, 0, 0, 0.90}, {0, 0, 0, 0, 0.95}, {0, 0, 0, 0, 0.88},
        {0, 0, 0, 0, 0.92},

        // YIELD (5) – triangular, red, sem número, sem seta
        {1, 0, 0, 0, 0.65}, {1, 0, 0, 0, 0.60}, {1, 0, 0, 0, 0.70},
        {1, 0, 0, 0, 0.55},

        // WARNING_AHEAD (6) – triangular, yellow, sem número, sem seta
        {1, 2, 0, 0, 0.55}, {1, 2, 0, 0, 0.50}, {1, 2, 0, 0, 0.60},
        {1, 2, 0, 0, 0.45},

        // PEDESTRIAN (7) – rectangular, blue, sem número, sem seta
        {3, 1, 0, 0, 0.60}, {3, 1, 0, 0, 0.55}, {3, 1, 0, 0, 0.65},

        // UNKNOWN (8) – combinações incomuns
        {3, 3, 1, 1, 0.30}, {0, 3, 1, 1, 0.20}, {1, 1, 1, 1, 0.10},
    };

    private static final int[] TRAINING_LABELS = {
        0, 0, 0, 0, 0,           // STOP
        1, 1, 1, 1, 1,           // SPEED_LIMIT
        2, 2, 2,                  // NO_TURN_LEFT
        3, 3, 3,                  // NO_TURN_RIGHT
        4, 4, 4, 4,               // NO_ENTRY
        5, 5, 5, 5,               // YIELD
        6, 6, 6, 6,               // WARNING_AHEAD
        7, 7, 7,                  // PEDESTRIAN
        8, 8, 8,                  // UNKNOWN
    };

    // ── Schema do DataFrame ─────────────────────────────────────────────────
    private static final StructType SCHEMA = DataTypes.struct(
        new StructField("shape",      DataTypes.DoubleType),
        new StructField("color",      DataTypes.DoubleType),
        new StructField("hasNumber",  DataTypes.DoubleType),
        new StructField("hasArrow",   DataTypes.DoubleType),
        new StructField("brightness", DataTypes.DoubleType),
        new StructField("label",      DataTypes.IntegerType)
    );

    // ── Construtor ─────────────────────────────────────────────────────────
    public SignClassifier() {
        log.info("[SignClassifier] Treinando RandomForest com {} exemplos sintéticos...",
                TRAINING_FEATURES.length);
        long t0 = System.currentTimeMillis();

        DataFrame df = buildDataFrame(TRAINING_FEATURES, TRAINING_LABELS);
        this.model  = RandomForest.fit(Formula.lhs("label"), df);

        long elapsed = System.currentTimeMillis() - t0;
        log.info("[SignClassifier] Modelo treinado em {}ms | {} árvores",
                elapsed, model.trees().length);
    }

    // ── Inferência ──────────────────────────────────────────────────────────
    /**
     * Classifica um sinal a partir de uma mensagem.
     *
     * @param message mensagem recebida do broker ou do mock
     * @return resultado da classificação com confiança e tempo de inferência
     */
    public SignClassification classify(TrafficSignMessage message) {
        long t0 = System.currentTimeMillis();

        double[] features = FeatureExtractor.extract(message);
        int      labelIdx = predictLabel(features);
        double   confidence = predictConfidence(features, labelIdx);
        Label    label   = safeLabel(labelIdx);

        long ms = System.currentTimeMillis() - t0;
        return SignClassification.of(message.getMessageId(), label, confidence, ms);
    }

    // ── Internos ────────────────────────────────────────────────────────────
    private int predictLabel(double[] features) {
        DataFrame single = buildDataFrame(new double[][]{features}, new int[]{0});
        int[] predictions = model.predict(single);
        return predictions[0];
    }

    private double predictConfidence(double[] features, int predictedLabel) {
        try {
            // Smile 3.x: predict com probabilidades requer Tuple, não DataFrame
            DataFrame single = buildDataFrame(new double[][]{features}, new int[]{0});
            Tuple tuple = single.get(0);
            double[] probs = new double[labelIndex.length];
            model.predict(tuple, probs);
            return Math.min(1.0, Math.max(0.0, probs[predictedLabel]));
        } catch (Exception e) {
            return 0.70;
        }
    }

    private Label safeLabel(int idx) {
        if (idx >= 0 && idx < labelIndex.length) return labelIndex[idx];
        return Label.UNKNOWN;
    }

    private static DataFrame buildDataFrame(double[][] features, int[] labels) {
        int n = features.length;

        double[] shape      = new double[n];
        double[] color      = new double[n];
        double[] hasNumber  = new double[n];
        double[] hasArrow   = new double[n];
        double[] brightness = new double[n];

        for (int i = 0; i < n; i++) {
            shape[i]      = features[i][0];
            color[i]      = features[i][1];
            hasNumber[i]  = features[i][2];
            hasArrow[i]   = features[i][3];
            brightness[i] = features[i][4];
        }

        return DataFrame.of(
            DoubleVector.of("shape",      shape),
            DoubleVector.of("color",      color),
            DoubleVector.of("hasNumber",  hasNumber),
            DoubleVector.of("hasArrow",   hasArrow),
            DoubleVector.of("brightness", brightness),
            IntVector.of("label",        labels)
        );
    }
}
