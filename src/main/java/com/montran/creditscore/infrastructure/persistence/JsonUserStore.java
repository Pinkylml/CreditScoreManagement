package com.montran.creditscore.infrastructure.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.montran.creditscore.infrastructure.persistence.dto.SystemContainerDto;
import com.montran.creditscore.infrastructure.persistence.dto.UserStorageDto;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @docs Concrete adapter utilizing the Google Gson library to manage lightweight JSON document storage.
 */
public class JsonUserStore extends AbstractFileUserStore {

    /** @docs File path target representing the primary JSON database location. */
    private static final String FILE_PATH = "users.json";

    /**
     * @docs Thread-safe Gson instance initialized statically.
     * This ensures it is available before the superclass constructor triggers readFromFile().
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * @docs Initializes the JSON persistence engine.
     */
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
            return (container != null && container.getUsers() != null) ? container.getUsers() : new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON database.", e);
        }
    }

    @Override
    protected void writeToFile(List<UserStorageDto> dtos) {
        SystemContainerDto container = new SystemContainerDto();
        container.setUsers(dtos);

        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            GSON.toJson(container, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to JSON database.", e);
        }
    }
}