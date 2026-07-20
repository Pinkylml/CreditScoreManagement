package com.montran.creditscore.infrastructure.persistence.dto;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

/**
 * Flat, mutable representation of a {@link com.montran.creditscore.domain.model.User}
 * used exclusively for serialization. Strips business rules so JAXB and Gson can
 * read and write it freely via reflection.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "User", propOrder = {
        "ssn",
        "name",
        "address",
        "email",
        "creditScore",
        "riskLevel",
        "totalCreditLimit",
        "creditHistory"
})
public class UserStorageDto {

    @XmlElement(name = "ssn", required = true)
    private String ssn;

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "address")
    private String address;

    @XmlElement(name = "creditScore")
    private double creditScore;

    @XmlElement(name = "riskLevel")
    private String riskLevel;

    /** Stored as a plain string to preserve {@link java.math.BigDecimal} precision without loss. */
    @XmlElement(name = "totalCreditLimit")
    private String totalCreditLimit;

    @XmlElementWrapper(name = "creditHistory")
    @XmlElement(name = "record")
    private List<CreditHistoryStorageDto> creditHistory = new ArrayList<>();

    @XmlElement(name = "email")
    private String email;

    public UserStorageDto() {
    }

    // getters and setters

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(double creditScore) {
        this.creditScore = creditScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<CreditHistoryStorageDto> getCreditHistory() {
        return creditHistory;
    }

    public void setCreditHistory(List<CreditHistoryStorageDto> creditHistory) {
        this.creditHistory = creditHistory;
    }

    public String getTotalCreditLimit() {
        return totalCreditLimit;
    }

    public void setTotalCreditLimit(String totalCreditLimit) {
        this.totalCreditLimit = totalCreditLimit;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
