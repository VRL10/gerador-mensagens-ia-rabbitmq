package br.com.sd.trabalho06.shared;

import java.util.UUID;

public record PlateMessage(
        UUID id,
        MessageKind kind,
        String plateText,
        VehicleType vehicleType,
        long createdAtEpochMillis,
        String imageBase64,
        int width,
        int height
) {
}