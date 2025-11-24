module gravador.java25 {
    // JavaFX
    requires javafx.controls;
    requires javafx.graphics;

    // Para System Tray e Audio (Microfone)
    requires java.desktop;

    // Necessária para usar o HttpClient nativo
    requires java.net.http;

    exports dev.jlucasbs.gravador;
}