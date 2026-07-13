package com.montran.creditscore.infrastructure.persistence;

import com.montran.creditscore.infrastructure.persistence.dto.SystemContainerDto;
import com.montran.creditscore.infrastructure.persistence.dto.UserStorageDto;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @docs Concrete adapter utilizing JAXB architecture to marshal/unmarshal data
 * structures to XML files.
 */

public class XmlUserStore extends AbstractFileUserStore {
    /** @docs File path target representing the primary XML database location. */
    private static final String FILE_PATH = "users.xml";

    /**
     * @docs Initializes the XML persistence engine and triggers the superclass cache load.
     */
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
            throw new RuntimeException("Failed to read XML database.", e);
        }
    }

    @Override
    protected void writeToFile(List<UserStorageDto> dtos) {
        SystemContainerDto container = new SystemContainerDto();
        container.setUsers(dtos);

        try {
            JAXBContext context = JAXBContext.newInstance(SystemContainerDto.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(container, new File(FILE_PATH));
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to write to XML database.", e);
        }
    }
}
