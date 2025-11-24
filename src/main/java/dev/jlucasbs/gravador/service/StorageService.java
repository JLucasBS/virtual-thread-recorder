package dev.jlucasbs.gravador.service;

import dev.jlucasbs.gravador.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class StorageService {
    private final File outputDirectory;
    private static final String DIR_NAME = "GravadorBuffer";

    public StorageService(AppConfig config) {
        this.outputDirectory = Path.of(config.userHome(), DIR_NAME).toFile();
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
    }

    public File createNewChunkFile(String sessionId, int chunkIndex) {
        String userName = System.getProperty("user.name");
        String fileName = String.format("%s_%s_p%03d.wav", userName, sessionId, chunkIndex);
        return new File(outputDirectory, fileName);
    }

    public List<File> findPendingFiles(File currentActiveFile) {
        try (Stream<Path> files = Files.list(outputDirectory.toPath())) {
            return files
                    .filter(p -> p.toString().endsWith(".wav"))
                    .map(Path::toFile)
                    .filter(file -> isNotActive(file, currentActiveFile))
                    .toList();
        } catch (IOException e) {
            System.err.println("Erro ao listar arquivos: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean isNotActive(File candidate, File active) {
        if (active == null) return true;
        return !candidate.getAbsolutePath().equals(active.getAbsolutePath());
    }

    public void deleteFile(File file) {
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) System.out.println("🗑️ Arquivo local removido: " + file.getName());
        }
    }
}