package com.montran.creditscore.domain.model;

import java.util.Date;
import java.util.UUID;
import com.montran.creditscore.domain.exception.InvalidCreditDataException;

/**
 * Immutable value object representing a single credit transaction in a user's history.
 * Dates are defensively copied on construction and retrieval to guard against external mutations.
 */
public final class CreditHistoryRecord {

    private final String transactionId;
    private final Date dueDate;

    /** Null if the transaction is still pending or has defaulted. */
    private final Date settlementDate;

    private final TransactionType transactionType;
    private final double amount;
    private final TransactionStatus status;

    /**
     * Creates a validated, immutable transaction record.
     *
     * @param transactionId   Unique ID for this record. A UUID is generated automatically if null or blank.
     * @param dueDate         The date the payment was due. Cannot be null.
     * @param settlementDate  The date the payment was made. Null for pending or defaulted payments.
     * @param transactionType The type of credit instrument. Cannot be null.
     * @param amount          The monetary amount involved. Must be non-negative.
     * @param status          The payment status. Cannot be null.
     * @throws IllegalArgumentException   if dueDate, transactionType, or status is null.
     * @throws InvalidCreditDataException if amount is negative.
     */
    public CreditHistoryRecord(String transactionId, Date dueDate, Date settlementDate, TransactionType transactionType,
            double amount, TransactionStatus status) {
        if (dueDate == null) {
            throw new IllegalArgumentException("Transaction due date cannot be null.");
        }
        if (transactionType == null) {
            throw new IllegalArgumentException("Transaction context type cannot be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Transaction settlement status cannot be null.");
        }
        if (amount < 0.0) {
            throw new InvalidCreditDataException("Historical financial transaction amount cannot be negative.");
        }

        this.transactionId = (transactionId != null && !transactionId.trim().isEmpty()) ? transactionId
                : UUID.randomUUID().toString();
        this.dueDate = new Date(dueDate.getTime());
        this.settlementDate = (settlementDate != null) ? new Date(settlementDate.getTime()) : null;
        this.transactionType = transactionType;
        this.amount = amount;
        this.status = status;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public Date getDueDate() {
        return new Date(this.dueDate.getTime());
    }

    public Date getSettlementDate() {
        return (this.settlementDate != null) ? new Date(this.settlementDate.getTime()) : null;
    }

    public TransactionType getTransactionType() {
        return this.transactionType;
    }

    public double getAmount() {
        return this.amount;
    }

    public TransactionStatus getStatus() {
        return this.status;
    }

    public boolean isDefaulted() {
        return this.status == TransactionStatus.DEFAULTED;
    }
}