package io.github.fal1winter.langgraph4j.persistence;

import io.github.fal1winter.langgraph4j.core.State;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * File-based state store implementation
 * Stores state as serialized objects in the filesystem
 *
 * @param <S> the state type
 */
public class FileStateStore<S extends State> implements StateStore<S> {

    private final Path storageDir;

    public FileStateStore(String storagePath) {
        this.storageDir = Paths.get(storagePath);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory: " + storagePath, e);
        }
    }

    @Override
    public void save(String checkpointId, S state) throws Exception {
        Path filePath = getFilePath(checkpointId);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(state);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public S load(String checkpointId) throws Exception {
        Path filePath = getFilePath(checkpointId);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Checkpoint not found: " + checkpointId);
        }
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filePath.toFile()))) {
            return (S) ois.readObject();
        }
    }

    @Override
    public boolean exists(String checkpointId) {
        return Files.exists(getFilePath(checkpointId));
    }

    @Override
    public void delete(String checkpointId) throws Exception {
        Path filePath = getFilePath(checkpointId);
        Files.deleteIfExists(filePath);
    }

    @Override
    public List<String> listCheckpoints() {
        try {
            return Files.list(storageDir)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".checkpoint"))
                    .map(name -> name.substring(0, name.length() - 11))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private Path getFilePath(String checkpointId) {
        return storageDir.resolve(checkpointId + ".checkpoint");
    }
}
