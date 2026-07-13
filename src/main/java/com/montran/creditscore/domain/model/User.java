package com.montran.creditscore.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @docs Domain Aggregate Root representing a distinct client entity within the system.
 * <p>This object encapsulates identity details, running evaluation metrics, and the collection
 * of associated credit events. State transitions are strictly governed via internal mutators
 * to ensure business invariance.</p>
 */
public class User {

    /** @docs The unique Social Security Number acts as the primary logical business key. */
    private final String ssn;

    /** @docs The legal registered name of the user entity. */
    private String name;

    /** @docs The current physical mailing address context. */
    private String address;

    /** @docs The dynamically calculated rating metric bounded between 0 and 100. */
    private double creditScore;

    /** @docs The resolved categorization bracket indicating portfolio default vulnerability. */
    private RiskLevel riskLevel;

    /** @docs The private list containing all chronological financial history logs. */
    private final List<CreditHistoryRecord> creditHistory;

    /** @docs The personal maximum credit limit assigned to this specific user profile. */
    private double totalCreditLimit;

    /**
     * @docs Initializes a fresh User instance with empty histories and baseline risk assignments.
     * @param ssn The unique identity token; must be non-empty and non-null.
     * @param name The individual's legal descriptive identity label.
     * @param address The primary residency location info.
     * @throws IllegalArgumentException if the provided SSN is null or blank.
     */
    public User(String ssn, String name, String address, double totalCreditLimit) {
        if (ssn == null || ssn.trim().isEmpty()) {
            throw new IllegalArgumentException("A unique, non-blank SSN identifier is mandatory.");
        }
        if (totalCreditLimit <= 0) {
            throw new IllegalArgumentException("Total credit limit must be greater than zero.");
        }
        this.ssn = ssn;
        this.name = name;
        this.address = address;
        this.creditScore = 0.0;
        this.riskLevel = RiskLevel.HIGH;
        this.creditHistory = new ArrayList<>();
        this.totalCreditLimit = totalCreditLimit;;
    }

    //getters
    public String getSsn() {
        return ssn;
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

    public double getCreditScore() {
        return creditScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<CreditHistoryRecord> getCreditHistory() {
        return Collections.unmodifiableList(this.creditHistory);
    }

    public double getTotalCreditLimit() { return totalCreditLimit; }

    //setters
    public void setAddress(String address) {
        this.address = address;
    }

    public void setCreditScore(double creditScore) {
        this.creditScore = creditScore;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void setTotalCreditLimit(double totalCreditLimit) { this.totalCreditLimit = totalCreditLimit; }


    /**
     * @docs Appends a validated transactional record directly to the structural
     * timeline sequence.
     * @param record The non-null transaction value object to store.
     * @throws IllegalArgumentException if the provided record reference is null.
     */
    public void addCreditRecord(CreditHistoryRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot append an uninitialized transaction entry reference.");
        }
        this.creditHistory.add(record);
    }

    /**
     * @docs Purges all historical record items stored inside the internal array list.
     */
    public void clearCreditHistory() {
        this.creditHistory.clear();
    }
}