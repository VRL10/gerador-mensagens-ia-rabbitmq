package br.com.sd.trabalho06.shared;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TemplateOcr {
    private static final Font TEMPLATE_FONT = new Font("Monospaced", Font.BOLD, 72);
    private static final int TEMPLATE_WIDTH = 50;
    private static final int TEMPLATE_HEIGHT = 92;
    private static final Map<Character, BufferedImage> TEMPLATES = buildTemplates();

    private TemplateOcr() {
    }

    public static String recognizeSevenCharacterPlate(BufferedImage plateImage) {
        BufferedImage textRegion = ImageFeatures.crop(plateImage, 42, 26, 336, 104);
        int segmentWidth = textRegion.getWidth() / 7;
        StringBuilder recognized = new StringBuilder();
        for (int index = 0; index < 7; index++) {
            int x = index * segmentWidth;
            BufferedImage segment = ImageFeatures.crop(textRegion, x, 0, segmentWidth, textRegion.getHeight());
            recognized.append(recognizeCharacter(segment));
        }
        return recognized.toString();
    }

    private static char recognizeCharacter(BufferedImage segment) {
        BufferedImage normalized = normalize(segment);
        double bestScore = Double.MAX_VALUE;
        char bestCharacter = '?';
        for (Map.Entry<Character, BufferedImage> entry : TEMPLATES.entrySet()) {
            double score = difference(normalized, entry.getValue());
            if (score < bestScore) {
                bestScore = score;
                bestCharacter = entry.getKey();
            }
        }
        return bestCharacter;
    }

    private static double difference(BufferedImage left, BufferedImage right) {
        double total = 0.0;
        for (int y = 0; y < TEMPLATE_HEIGHT; y++) {
            for (int x = 0; x < TEMPLATE_WIDTH; x++) {
                int leftValue = left.getRGB(x, y) & 0xFF;
                int rightValue = right.getRGB(x, y) & 0xFF;
                double delta = leftValue - rightValue;
                total += delta * delta;
            }
        }
        return total / (TEMPLATE_WIDTH * TEMPLATE_HEIGHT);
    }

    private static BufferedImage normalize(BufferedImage source) {
        BufferedImage target = new BufferedImage(TEMPLATE_WIDTH, TEMPLATE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, TEMPLATE_WIDTH, TEMPLATE_HEIGHT);
        graphics.drawImage(source, 0, 0, TEMPLATE_WIDTH, TEMPLATE_HEIGHT, null);
        graphics.dispose();
        return threshold(target);
    }

    private static BufferedImage threshold(BufferedImage image) {
        BufferedImage binary = new BufferedImage(TEMPLATE_WIDTH, TEMPLATE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < TEMPLATE_HEIGHT; y++) {
            for (int x = 0; x < TEMPLATE_WIDTH; x++) {
                int value = image.getRGB(x, y) & 0xFF;
                int threshold = value < 150 ? 0 : 255;
                int rgb = (threshold << 16) | (threshold << 8) | threshold;
                binary.setRGB(x, y, rgb);
            }
        }
        return binary;
    }

    private static Map<Character, BufferedImage> buildTemplates() {
        Map<Character, BufferedImage> templates = new LinkedHashMap<>();
        for (char character : "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()) {
            templates.put(character, renderTemplate(character));
        }
        return templates;
    }

    private static BufferedImage renderTemplate(char character) {
        BufferedImage image = new BufferedImage(TEMPLATE_WIDTH, TEMPLATE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, TEMPLATE_WIDTH, TEMPLATE_HEIGHT);
        graphics.setColor(Color.BLACK);
        graphics.setFont(TEMPLATE_FONT);
        FontMetrics metrics = graphics.getFontMetrics();
        String text = String.valueOf(character);
        int textWidth = metrics.stringWidth(text);
        int x = (TEMPLATE_WIDTH - textWidth) / 2;
        int y = (TEMPLATE_HEIGHT + metrics.getAscent()) / 2 - 8;
        graphics.drawString(text, x, y);
        graphics.dispose();
        return threshold(image);
    }
}