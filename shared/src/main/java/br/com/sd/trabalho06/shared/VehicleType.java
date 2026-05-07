package br.com.sd.trabalho06.shared;

public enum VehicleType {
    CAR,
    MOTORCYCLE,
    TRUCK;

    public String displayName() {
        return switch (this) {
            case CAR -> "carro";
            case MOTORCYCLE -> "moto";
            case TRUCK -> "caminhao";
        };
    }
}