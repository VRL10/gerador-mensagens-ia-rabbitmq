package br.com.sd.trabalho06.shared;

import java.util.UUID;

public record SignMessage(
        UUID id,
        MessageKind kind,
        SignType signType,
        long createdAtEpochMillis,
        String imageBase64,
        int width,
        int height
) {
}