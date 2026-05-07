package br.com.sd.trabalho06.shared;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public final class SyntheticImageFactory {
    private static final int PLATE_WIDTH = 420;
    private static final int PLATE_HEIGHT = 160;
    private static final int SIGN_WIDTH = 256;
    private static final int SIGN_HEIGHT = 256;

    private static final Font PLATE_FONT = new Font("Monospaced", Font.BOLD, 72);
    private static final Font SIGN_FONT = new Font("SansSerif", Font.BOLD, 44);

    private SyntheticImageFactory() {
    }

    public static BufferedImage createPlateImage(String plateText, VehicleType vehicleType) {
        BufferedImage image = new BufferedImage(PLATE_WIDTH, PLATE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        configureGraphics(graphics);

        graphics.setColor(new Color(246, 246, 240));
        graphics.fillRect(0, 0, PLATE_WIDTH, PLATE_HEIGHT);

        graphics.setColor(switch (vehicleType) {
            case CAR -> new Color(40, 90, 220);
            case MOTORCYCLE -> new Color(24, 160, 90);
            case TRUCK -> new Color(235, 120, 35);
        });
        graphics.setStroke(new BasicStroke(12f));
        graphics.drawRoundRect(10, 10, PLATE_WIDTH - 20, PLATE_HEIGHT - 20, 26, 26);

        graphics.setColor(new Color(28, 28, 28));
        graphics.setFont(PLATE_FONT);
        FontMetrics metrics = graphics.getFontMetrics();
        int textWidth = metrics.stringWidth(plateText);
        int textX = (PLATE_WIDTH - textWidth) / 2;
        int textY = (PLATE_HEIGHT + metrics.getAscent()) / 2 - 10;
        graphics.drawString(plateText, textX, textY);

        graphics.dispose();
        return image;
    }

    public static BufferedImage createSignImage(SignType signType) {
        BufferedImage image = new BufferedImage(SIGN_WIDTH, SIGN_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        configureGraphics(graphics);

        graphics.setColor(new Color(245, 245, 245));
        graphics.fillRect(0, 0, SIGN_WIDTH, SIGN_HEIGHT);

        switch (signType) {
            case STOP -> drawStopSign(graphics);
            case SPEED_LIMIT -> drawSpeedLimit(graphics);
            case NO_TURN -> drawNoTurn(graphics);
        }

        graphics.dispose();
        return image;
    }

    public static String createRandomPlateText(Random random) {
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        char[] digits = "0123456789".toCharArray();
        return "" + alphabet[random.nextInt(alphabet.length)]
                + alphabet[random.nextInt(alphabet.length)]
                + alphabet[random.nextInt(alphabet.length)]
                + digits[random.nextInt(digits.length)]
                + alphabet[random.nextInt(alphabet.length)]
                + digits[random.nextInt(digits.length)]
                + digits[random.nextInt(digits.length)];
    }

    public static VehicleType randomVehicleType(Random random) {
        VehicleType[] values = VehicleType.values();
        return values[random.nextInt(values.length)];
    }

    public static SignType randomSignType(Random random) {
        SignType[] values = SignType.values();
        return values[random.nextInt(values.length)];
    }

    private static void drawStopSign(Graphics2D graphics) {
        graphics.setColor(new Color(201, 35, 35));
        Shape octagon = createOctagon(128, 128, 88);
        graphics.fill(octagon);
        graphics.setColor(Color.WHITE);
        graphics.setFont(SIGN_FONT);
        drawCenteredText(graphics, "PARE", 128, 138);
    }

    private static void drawSpeedLimit(Graphics2D graphics) {
        graphics.setColor(Color.WHITE);
        graphics.fillOval(42, 42, 172, 172);
        graphics.setColor(new Color(204, 34, 34));
        graphics.setStroke(new BasicStroke(18f));
        graphics.drawOval(42, 42, 172, 172);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 66));
        drawCenteredText(graphics, "60", 128, 140);
    }

    private static void drawNoTurn(Graphics2D graphics) {
        graphics.setColor(new Color(33, 93, 214));
        graphics.fillOval(34, 34, 188, 188);
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(86, 74, 170, 182);
        graphics.drawLine(92, 86, 178, 174);
        graphics.setStroke(new BasicStroke(14f));
        graphics.drawOval(34, 34, 188, 188);
    }

    private static void drawCenteredText(Graphics2D graphics, String text, int centerX, int baselineY) {
        FontMetrics metrics = graphics.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        graphics.drawString(text, centerX - textWidth / 2, baselineY);
    }

    private static Shape createOctagon(int centerX, int centerY, int radius) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(22.5 + 45.0 * i);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.closePath();
        return path;
    }

    private static void configureGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setTransform(AffineTransform.getTranslateInstance(0, 0));
    }
}