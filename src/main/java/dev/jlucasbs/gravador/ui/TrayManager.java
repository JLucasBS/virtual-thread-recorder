package dev.jlucasbs.gravador.ui;

import dev.jlucasbs.gravador.service.AudioService;
import javafx.application.Platform;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TrayManager {
    private final AudioService audioService;
    private TrayIcon trayIcon;

    public TrayManager(AudioService audioService) {
        this.audioService = audioService;
    }

    public void init() {
        if (!SystemTray.isSupported()) return;

        var tray = SystemTray.getSystemTray();
        var popup = createMenu();

        trayIcon = new TrayIcon(drawIcon(Color.GRAY), "Gravador", popup);
        trayIcon.setImageAutoSize(true);

        // Duplo clique alterna estado
        trayIcon.addActionListener(e -> toggleRecording());

        try {
            tray.add(trayIcon);
            updateStatus(true); // Inicia como gravando
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void updateStatus(boolean isRecording) {
        if (trayIcon == null) return;

        if (isRecording) {
            updateIcon(Color.RED, "Gravando...");
        } else {
            updateIcon(Color.GRAY, "Parado");
        }
    }

    private void toggleRecording() {
        // Lógica simplificada de toggle visual
        // Em um app real, você checaria o estado do serviço
        audioService.stopRecording(); // Exemplo
    }

    private PopupMenu createMenu() {
        var popup = new PopupMenu();
        var exitItem = new MenuItem("Sair");

        exitItem.addActionListener(e -> {
            audioService.forceStop();
            System.exit(0);
        });

        popup.add(exitItem);
        return popup;
    }

    private void updateIcon(Color color, String tooltip) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            trayIcon.setImage(drawIcon(color));
            trayIcon.setToolTip(tooltip);
        });
    }

    private Image drawIcon(Color color) {
        int size = 16;
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillOval(1, 1, size - 2, size - 2);
        g.dispose();
        return image;
    }
}