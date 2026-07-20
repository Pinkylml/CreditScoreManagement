package com.montran.creditscore.infrastructure.persistence;

import com.montran.creditscore.domain.exception.PersistenceException;
import com.montran.creditscore.infrastructure.persistence.dto.SystemContainerDto;
import com.montran.creditscore.infrastructure.persistence.dto.UserStorageDto;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * File-backed user store that reads and writes {@code users.xml} using JAXB.
 *
 * <h3>Atomic writes</h3>
 * <p>Data is marshalled to a temporary file ({@code users.xml.tmp}) first.
 * Once the stream is fully flushed and closed, {@link Files#move} with
 * {@link StandardCopyOption#ATOMIC_MOVE} promotes the temp file to the real path.
 * If the OS does not support atomic cross-directory moves,
 * {@link AtomicMoveNotSupportedException} is caught and a non-atomic
 * {@link StandardCopyOption#REPLACE_EXISTING} move is used as a fallback.
 * This prevents a JVM crash mid-write from leaving a corrupt or truncated XML file.</p>
 */
public class XmlUserStore extends AbstractFileUserStore {

    private static final String FILE_PATH = "users.xml";
    private static final String TEMP_PATH = FILE_PATH + ".tmp";

    public XmlUserStore() {
        super();
    }

    @Override
    protected List<UserStorageDto> readFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            JAXBContext context = JAXBContext.newInstance(SystemContainerDto.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SystemContainerDto container = (SystemContainerDto) unmarshaller.unmarshal(file);
            return container.getUsers() != null ? container.getUsers() : new ArrayList<>();
        } catch (JAXBException e) {
            throw new PersistenceException("Failed to read XML storage file '" + FILE_PATH + "'.", e);
        }
    }

    /**
     * Marshals {@code dtos} to a temporary file, then atomically renames it over the real file.
     * Any JAXB or I/O failure wraps in {@link PersistenceException}.
     */
    @Override
    protected void writeToFile(List<UserStorageDto> dtos) {
        SystemContainerDto container = new SystemContainerDto();
        container.setUsers(dtos);

        Path tempPath = Paths.get(TEMP_PATH);
        Path targetPath = Paths.get(FILE_PATH);

        // 1. Write to the temp file (fully flushed on JAXB marshal).
        try {
            JAXBContext context = JAXBContext.newInstance(SystemContainerDto.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(container, tempPath.toFile());
        } catch (JAXBException e) {
            throw new PersistenceException("Failed to serialize data to temp XML file '" + TEMP_PATH + "'.", e);
        }

        // 2. Atomically promote the temp file to the target path.
        atomicMove(tempPath, targetPath);
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
