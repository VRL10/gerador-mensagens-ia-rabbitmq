package br.com.sd.trabalho06.shared;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ImageCodec {
    private ImageCodec() {
    }

    public static String encodeBase64Png(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel codificar a imagem", exception);
        }
    }

    public static BufferedImage decodeBase64Png(String base64) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64))) {
            return ImageIO.read(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel decodificar a imagem", exception);
        }
    }
}