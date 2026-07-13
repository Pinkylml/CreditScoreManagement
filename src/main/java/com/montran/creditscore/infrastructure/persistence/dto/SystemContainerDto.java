package com.montran.creditscore.infrastructure.persistence.dto;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @docs Root document object framework container acting as the physical entry file
 * translation boundary.
 * <p><b>Design Justification:</b> Maps directly to the root XML element or top-level
 * JSON wrapper array structure, maintaining full structural isolation for our data
 * collections on disk.</p>
 */
@XmlRootElement(name = "creditSystem")
@XmlAccessorType(XmlAccessType.FIELD)
public class SystemContainerDto {
    /** @docs Centralized user collection storage block matching
     * data allocations on disk. */
    @XmlElementWrapper(name = "users")
    @XmlElement(name = "user")
    private List<UserStorageDto> users = new ArrayList<>();


    public SystemContainerDto() {
    }

    //getters and setters
    public List<UserStorageDto> getUsers() {
        return users;
    }

    public void setUsers(List<UserStorageDto> users) {
        this.users = users;
    }
}
