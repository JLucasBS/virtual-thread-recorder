package dev.jlucasbs.gravador.config;

import java.time.Duration;

public record AppConfig(
        String backendUrl,
        int chunkDurationSeconds,
        long retryIntervalMs,
        String userHome
) {
    // Factory method com valores padrão
    public static AppConfig loadDefault() {
        return new AppConfig(
                "https://api.seubackend.com/generate-signed-url",
                600, // 10 minutos
                Duration.ofMinutes(30).toMillis(),
                System.getProperty("user.home")
        );
    }
}