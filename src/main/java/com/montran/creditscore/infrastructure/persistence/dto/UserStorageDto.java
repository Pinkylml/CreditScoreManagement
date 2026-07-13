package com.montran.creditscore.infrastructure.persistence.dto;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

/**
 * @docs Flattened Data Transfer Object representation of the User Aggregate Root optimized for serialization hierarchies.
 * <p><b>Design Justification:</b> Strips out thread-safe primitives, business rules, and encapsulation locks, exposing a pure mutable metadata structure matching flat JSON and XML schemas perfectly.</p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "User", propOrder = {
        "ssn",
        "name",
        "address",
        "creditScore",
        "riskLevel",
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

    @XmlElement(name = "totalCreditLimit")
    private double totalCreditLimit;

    @XmlElementWrapper(name = "creditHistory")
    @XmlElement(name = "record")
    private List<CreditHistoryStorageDto> creditHistory = new ArrayList<>();

    public UserStorageDto() {
    }

    //getters and setters


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

    public double getTotalCreditLimit() {
        return totalCreditLimit;
    }

    public void setTotalCreditLimit(double totalCreditLimit) {
        this.totalCreditLimit = totalCreditLimit;
    }
}
