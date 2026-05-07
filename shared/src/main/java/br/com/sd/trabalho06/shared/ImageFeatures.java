package br.com.sd.trabalho06.shared;

import java.awt.Color;
import java.awt.image.BufferedImage;

public final class ImageFeatures {
    private ImageFeatures() {
    }

    public static double[] vehicleFeatures(BufferedImage image) {
        return new double[] {
                averageRed(image),
                averageGreen(image),
                averageBlue(image),
                widthToHeight(image)
        };
    }

    public static double[] signFeatures(BufferedImage image) {
        return new double[] {
                averageRed(image),
                averageGreen(image),
                averageBlue(image),
                redPixelRatio(image),
                bluePixelRatio(image),
                darkPixelRatio(image)
        };
    }

    public static BufferedImage crop(BufferedImage image, int x, int y, int width, int height) {
        return image.getSubimage(x, y, width, height);
    }

    private static double averageRed(BufferedImage image) {
        return channelAverage(image, 16);
    }

    private static double averageGreen(BufferedImage image) {
        return channelAverage(image, 8);
    }

    private static double averageBlue(BufferedImage image) {
        return channelAverage(image, 0);
    }

    private static double channelAverage(BufferedImage image, int shift) {
        long total = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                total += (rgb >> shift) & 0xFF;
            }
        }
        return total / (double) (width * height * 255.0);
    }

    private static double widthToHeight(BufferedImage image) {
        return image.getWidth() / (double) image.getHeight();
    }

    private static double redPixelRatio(BufferedImage image) {
        return colorRatio(image, new Color(170, 40, 40), 60);
    }

    private static double bluePixelRatio(BufferedImage image) {
        return colorRatio(image, new Color(40, 70, 170), 60);
    }

    private static double darkPixelRatio(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int darkPixels = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                if (color.getRed() + color.getGreen() + color.getBlue() < 320) {
                    darkPixels++;
                }
            }
        }
        return darkPixels / (double) (width * height);
    }

    private static double colorRatio(BufferedImage image, Color target, int tolerance) {
        int width = image.getWidth();
        int height = image.getHeight();
        int matches = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color current = new Color(image.getRGB(x, y));
                int distance = Math.abs(current.getRed() - target.getRed())
                        + Math.abs(current.getGreen() - target.getGreen())
                        + Math.abs(current.getBlue() - target.getBlue());
                if (distance <= tolerance) {
                    matches++;
                }
            }
        }
        return matches / (double) (width * height);
    }
}