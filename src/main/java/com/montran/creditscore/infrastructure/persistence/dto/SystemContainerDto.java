package com.montran.creditscore.infrastructure.persistence.dto;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Top-level wrapper DTO that maps to the XML root element ({@code <creditSystem>})
 * and the Gson top-level object. Holds the full list of user records for serialization.
 */
@XmlRootElement(name = "creditSystem")
@XmlAccessorType(XmlAccessType.FIELD)
public class SystemContainerDto {

    @XmlElementWrapper(name = "users")
    @XmlElement(name = "user")
    private List<UserStorageDto> users = new ArrayList<>();

    public SystemContainerDto() {
    }

    public List<UserStorageDto> getUsers() {
        return users;
    }

    public void setUsers(List<UserStorageDto> users) {
        this.users = users;
    }
}
