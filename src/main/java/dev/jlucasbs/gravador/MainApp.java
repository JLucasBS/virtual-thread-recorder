package dev.jlucasbs.gravador;

import dev.jlucasbs.gravador.config.AppConfig;
import dev.jlucasbs.gravador.service.AudioService;
import dev.jlucasbs.gravador.service.NetworkService;
import dev.jlucasbs.gravador.service.StorageService;
import dev.jlucasbs.gravador.ui.TrayManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.File;

public class MainApp extends Application {

    private AudioService audioService;
    private StorageService storageService;
    private NetworkService networkService;

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);

        // 1. Inicializa Configuração e Serviços (Injeção de Dependência)
        var config = AppConfig.loadDefault();
        storageService = new StorageService(config);
        networkService = new NetworkService(config);
        audioService = new AudioService(config, storageService, networkService);
        var trayManager = new TrayManager(audioService);

        // 2. Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("⚠️ Shutdown detectado.");
            audioService.forceStop();
        }));

        // 3. Inicia o Scheduler de Retry (Processo em Background)
        startRetryScheduler(config.retryIntervalMs());

        // 4. Inicia UI e Gravação
        javax.swing.SwingUtilities.invokeLater(() -> {
            trayManager.init();
            audioService.startRecording();
            trayManager.updateStatus(true);
        });
    }

    private void startRetryScheduler(long intervalMs) {
        Thread.ofVirtual().name("Retry-Scheduler").start(() -> {
            System.out.println("⏰ Scheduler iniciado.");
            while (true) {
                try {
                    processPendingFiles();
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    private void processPendingFiles() {
        System.out.println("🔎 Verificando pendências...");
        File activeFile = audioService.getCurrentActiveFile();

        var pendingFiles = storageService.findPendingFiles(activeFile);

        for (File file : pendingFiles) {
            // Dispara em V-Threads paralelas para não bloquear o scheduler
            Thread.ofVirtual().start(() -> {
                boolean success = networkService.uploadFile(file);
                if (success) storageService.deleteFile(file);
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}