package com.montran.creditscore.domain.model;

import java.util.Date;

/**
 * @docs A Domain Value Object representing an immutable financial transaction entry.
 * <p><b>Thread-Safety Justification:</b> This class is explicitly immutable.
 * By enforcing a final structure and deep-copying all mutable references,
 * instances can be safely shared across calculation threads without synchronization overhead.</p>
 */

public final class CreditHistoryRecord {

    /** @docs The calendar date when this transaction record occurred. */
    private final Date date;

    /** @docs The structural financial category of this financial record. */
    private final TransactionType transactionType;

    /** @docs The total monetary balance or limit allocated to this transaction. */
    private final double amount;

    /** @docs The current repayment or operational status of this history element. */
    private final TransactionStatus status;

    /**
     * @docs Constructs a validated, unmodifiable historical transaction record.
     * @param date The date of the event; cannot be null.
     * @param transactionType The financial mechanism category; cannot be null.
     * @param amount The monetary sum involved; must be non-negative.
     * @param status The settlement status; cannot be null.
     * @throws IllegalArgumentException if any object parameters are null or amount is negative.
     */

    public CreditHistoryRecord(Date date, TransactionType transactionType, double amount, TransactionStatus status) {

        if(date==null){
            throw new IllegalArgumentException("Transaction entry date cannot be null.");
        }
        if(transactionType== null){
            throw new IllegalArgumentException("Transaction context type cannot be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Transaction settlement status cannot be null.");
        }
        if (amount < 0.0) {
            throw new IllegalArgumentException("Historical financial transaction amount cannot be negative.");
        }

        this.date = new Date(date.getTime());
        this.transactionType = transactionType;
        this.amount = amount;
        this.status = status;
    }

    //Getters


    public Date getDate() {
        return date;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    /**
     * @docs Utility evaluation flag indicating if this specific record counts as a default breach.
     * @return true if the status equals TransactionStatus.DEFAULTED, false otherwise.
     */
    public boolean isDefaulted() {
        return this.status == TransactionStatus.DEFAULTED;
    }

}
