package com.rheosim.infrastructure.experiment.adapter;

import com.rheosim.domain.experiment.port.FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path storageRoot;

    public LocalFileStorageAdapter(@Value("${rheosim.storage.path:./data/uploads}") String storagePath) {
        this.storageRoot = Path.of(storagePath);
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory: " + storagePath, e);
        }
    }

    @Override
    public String store(UUID datasetId, String fileName, InputStream content) {
        Path datasetDir = storageRoot.resolve(datasetId.toString());
        try {
            Files.createDirectories(datasetDir);
            Path filePath = datasetDir.resolve(sanitizeFileName(fileName));
            Files.copy(content, filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + fileName, e);
        }
    }

    @Override
    public InputStream load(String storagePath) {
        Path filePath = Path.of(storagePath);
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found: " + storagePath);
        }
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load file: " + storagePath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path filePath = Path.of(storagePath);
            Files.deleteIfExists(filePath);
            Path parent = filePath.getParent();
            if (parent != null && Files.isDirectory(parent) && isDirectoryEmpty(parent)) {
                Files.deleteIfExists(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete file: " + storagePath, e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        return Files.exists(Path.of(storagePath));
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean isDirectoryEmpty(Path dir) throws IOException {
        try (var entries = Files.list(dir)) {
            return entries.findFirst().isEmpty();
        }
    }
}
