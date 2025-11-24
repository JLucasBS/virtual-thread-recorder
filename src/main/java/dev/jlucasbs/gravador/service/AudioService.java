package dev.jlucasbs.gravador.service;

import dev.jlucasbs.gravador.config.AppConfig;

import javax.sound.sampled.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioService {
    private final AppConfig config;
    private final StorageService storageService;
    private final NetworkService networkService;

    // Executor dedicado para Uploads (Virtual Threads)
    private final ExecutorService uploadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile boolean isRecording = false;
    private volatile File currentActiveFile = null;
    private TargetDataLine targetLine;

    public AudioService(AppConfig config, StorageService storage, NetworkService network) {
        this.config = config;
        this.storageService = storage;
        this.networkService = network;
    }

    public void startRecording() {
        if (isRecording) return;

        try {
            var format = getAudioFormat();
            var info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) return;

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();

            isRecording = true;

            // Inicia loop em Thread Daemon
            Thread.ofPlatform().daemon().start(() -> captureLoop(targetLine, format));
            System.out.println("🎙️ Gravação iniciada.");

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        closeLine();
        currentActiveFile = null;
        System.out.println("⏹️ Gravação parada.");
    }

    // Usado pelo Shutdown Hook
    public void forceStop() {
        isRecording = false;
        closeLine();
    }

    public File getCurrentActiveFile() {
        return currentActiveFile;
    }

    private void closeLine() {
        if (targetLine != null && targetLine.isOpen()) {
            targetLine.stop();
            targetLine.close();
        }
    }

    private void captureLoop(TargetDataLine line, AudioFormat format) {
        String sessionId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        long bytesPerChunk = calculateBytesPerChunk(format);
        byte[] buffer = new byte[8192];
        int chunkIndex = 0;

        try {
            while (isRecording) {
                chunkIndex++;
                File file = storageService.createNewChunkFile(sessionId, chunkIndex);
                currentActiveFile = file;

                // Escreve no disco
                try (var fos = new FileOutputStream(file);
                     var bos = new BufferedOutputStream(fos)) {

                    long bytesWritten = 0;
                    while (isRecording && bytesWritten < bytesPerChunk) {
                        int read = line.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            bos.write(buffer, 0, read);
                            bytesWritten += read;
                        }
                    }
                }

                // Dispara upload assíncrono
                if (file.exists() && file.length() > 0) {
                    final File fileToUpload = file;
                    uploadExecutor.submit(() -> handleUpload(fileToUpload));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeLine();
        }
    }

    private void handleUpload(File file) {
        boolean success = networkService.uploadFile(file);
        if (success) {
            storageService.deleteFile(file);
        }
    }

    private long calculateBytesPerChunk(AudioFormat format) {
        int bytesPerSecond = (int) (format.getFrameRate() * format.getFrameSize());
        return (long) bytesPerSecond * config.chunkDurationSeconds();
    }

    private AudioFormat getAudioFormat() {
        return new AudioFormat(44100.0F, 16, 1, true, false);
    }
}