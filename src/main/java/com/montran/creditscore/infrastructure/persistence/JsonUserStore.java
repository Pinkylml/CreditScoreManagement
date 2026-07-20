package com.montran.creditscore.infrastructure.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.montran.creditscore.domain.exception.PersistenceException;
import com.montran.creditscore.infrastructure.persistence.dto.SystemContainerDto;
import com.montran.creditscore.infrastructure.persistence.dto.UserStorageDto;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * File-backed user store that reads and writes {@code users.json} using Gson.
 *
 * <h3>Atomic writes</h3>
 * <p>JSON is written to a temporary file ({@code users.json.tmp}) first.
 * Once the {@link FileWriter} is fully flushed and closed, {@link Files#move} with
 * {@link StandardCopyOption#ATOMIC_MOVE} promotes the temp file to the real path.
 * If the OS does not support atomic cross-directory moves,
 * {@link AtomicMoveNotSupportedException} is caught and a non-atomic
 * {@link StandardCopyOption#REPLACE_EXISTING} move is used as a fallback.
 * This prevents a JVM crash mid-write from leaving a corrupt or truncated JSON file.</p>
 */
public class JsonUserStore extends AbstractFileUserStore {

    private static final String FILE_PATH = "users.json";
    private static final String TEMP_PATH = FILE_PATH + ".tmp";

    /**
     * Initialized statically to ensure it is ready before the superclass constructor
     * triggers {@link #readFromFile()} during object creation.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public JsonUserStore() {
        super();
    }

    @Override
    protected List<UserStorageDto> readFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(file)) {
            SystemContainerDto container = GSON.fromJson(reader, SystemContainerDto.class);
            return (container != null && container.getUsers() != null)
                    ? container.getUsers() : new ArrayList<>();
        } catch (IOException e) {
            throw new PersistenceException("Failed to read JSON storage file '" + FILE_PATH + "'.", e);
        }
    }

    /**
     * Serializes {@code dtos} to a temporary file, then atomically renames it over the real file.
     * Any I/O failure wraps in {@link PersistenceException}.
     */
    @Override
    protected void writeToFile(List<UserStorageDto> dtos) {
        SystemContainerDto container = new SystemContainerDto();
        container.setUsers(dtos);

        // 1. Write to the temp file; try-with-resources ensures the stream is fully flushed/closed.
        try (FileWriter writer = new FileWriter(TEMP_PATH)) {
            GSON.toJson(container, writer);
            writer.flush();
        } catch (IOException e) {
            throw new PersistenceException("Failed to serialize data to temp JSON file '" + TEMP_PATH + "'.", e);
        }

        // 2. Atomically promote the temp file to the target path.
        atomicMove(Paths.get(TEMP_PATH), Paths.get(FILE_PATH));
    }

    /**
     * Attempts an {@link StandardCopyOption#ATOMIC_MOVE}; falls back to
     * {@link StandardCopyOption#REPLACE_EXISTING} if the OS does not support it.
     */
    private void atomicMove(Path source, Path target) {
        try {
            Files.move(source, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException fallback) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new PersistenceException(
                        "Failed to promote temp file '" + source + "' to '" + target + "' (non-atomic fallback).", e);
            }
        } catch (IOException e) {
            throw new PersistenceException(
                    "Failed to atomically promote temp file '" + source + "' to '" + target + "'.", e);
        }
    }
}