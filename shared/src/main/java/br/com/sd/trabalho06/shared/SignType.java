package br.com.sd.trabalho06.shared;

public enum SignType {
    STOP,
    SPEED_LIMIT,
    NO_TURN;

    public String displayName() {
        return switch (this) {
            case STOP -> "pare";
            case SPEED_LIMIT -> "velocidade_maxima";
            case NO_TURN -> "proibido_virar";
        };
    }
}