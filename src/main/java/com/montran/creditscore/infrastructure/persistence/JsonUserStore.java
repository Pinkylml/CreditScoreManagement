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

/** File-backed user store that reads and writes {@code users.json} using Gson. */
public class JsonUserStore extends AbstractFileUserStore {

    private static final String FILE_PATH = "users.json";

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