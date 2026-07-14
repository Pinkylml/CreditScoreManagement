package com.montran.creditscore.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.montran.creditscore.domain.exception.InvalidCreditDataException;

/**
 * Aggregate root for a credit system customer.
 * Holds the user's identity info, current credit score, risk level, and full transaction history.
 * Business rules (blank SSN, non-positive limit) are enforced at construction time.
 */
public class User {

    private final String ssn;
    private String name;
    private String address;
    private double creditScore;
    private RiskLevel riskLevel;
    private final List<CreditHistoryRecord> creditHistory;
    private double totalCreditLimit;
    private String email;

    /**
     * Creates a new user with a blank credit history and an initial risk level of HIGH.
     *
     * @param ssn              Unique identifier for the user. Cannot be null or blank.
     * @param name             Full name.
     * @param address          Mailing address.
     * @param email            Contact email.
     * @param totalCreditLimit The total credit limit assigned. Must be greater than zero.
     * @throws IllegalArgumentException   if the SSN is null or blank.
     * @throws InvalidCreditDataException if the credit limit is zero or negative.
     */
    public User(String ssn, String name, String address, String email, double totalCreditLimit) {
        if (ssn == null || ssn.trim().isEmpty()) {
            throw new IllegalArgumentException("A unique, non-blank SSN identifier is mandatory.");
        }
        if (totalCreditLimit <= 0) {
            throw new InvalidCreditDataException("Total credit limit must be greater than zero.");
        }
        this.ssn = ssn;
        this.name = name;
        this.address = address;
        this.email = email;
        this.creditScore = 0.0;
        this.riskLevel = RiskLevel.HIGH;
        this.creditHistory = new ArrayList<>();
        this.totalCreditLimit = totalCreditLimit;
    }

    // getters
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

    /** Returns an unmodifiable view of the credit history to prevent external mutations. */
    public List<CreditHistoryRecord> getCreditHistory() {
        return Collections.unmodifiableList(this.creditHistory);
    }

    public double getTotalCreditLimit() {
        return totalCreditLimit;
    }

    public String getEmail() {
        return email;
    }

    // setters
    public void setAddress(String address) {
        this.address = address;
    }

    public void setCreditScore(double creditScore) {
        this.creditScore = creditScore;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void setTotalCreditLimit(double totalCreditLimit) {
        this.totalCreditLimit = totalCreditLimit;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Adds a new transaction to the user's credit history.
     *
     * @param record The transaction to add. Cannot be null.
     * @throws IllegalArgumentException if record is null.
     */
    public void addCreditRecord(CreditHistoryRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot append an uninitialized transaction entry reference.");
        }
        this.creditHistory.add(record);
    }

    /** Clears all transactions from the user's credit history. */
    public void clearCreditHistory() {
        this.creditHistory.clear();
    }

    /**
     * Updates the user's name, address, and email. Silently ignores null or blank values
     * so callers can do partial updates without overwriting existing data.
     *
     * @param name    Updated name (ignored if blank).
     * @param address Updated address (ignored if blank).
     * @param email   Updated email (ignored if blank).
     */
    public void updateProfile(String name, String address, String email) {
        if (name != null && !name.trim().isEmpty())
            this.name = name;
        if (address != null && !address.trim().isEmpty())
            this.address = address;
        if (email != null && !email.trim().isEmpty())
            this.email = email;
    }

    /**
     * Removes a transaction from credit history by its ID.
     *
     * @param transactionId The ID of the record to remove.
     * @return {@code true} if a record was found and removed, {@code false} otherwise.
     */
    public boolean removeCreditRecord(String transactionId) {
        return this.creditHistory.removeIf(record -> record.getTransactionId().equals(transactionId));
    }

    /**
     * Replaces a transaction in credit history with updated data.
     *
     * @param transactionId The ID of the record to replace.
     * @param updatedRecord The new record data.
     * @return {@code true} if the record was found and replaced, {@code false} otherwise.
     */
    public boolean updateCreditRecord(String transactionId, CreditHistoryRecord updatedRecord) {
        for (int i = 0; i < this.creditHistory.size(); i++) {
            if (this.creditHistory.get(i).getTransactionId().equals(transactionId)) {
                this.creditHistory.set(i, updatedRecord);
                return true;
            }
        }
        return false;
    }
}