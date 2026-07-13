package com.montran.creditscore.domain.model;

import java.util.Date;

/**
 * @docs A Domain Value Object representing an immutable financial transaction
 *       entry.
 *       <p>
 *       <b>Thread-Safety Justification:</b> This class is explicitly immutable.
 *       By enforcing a final structure and deep-copying all mutable references,
 *       instances can be safely shared across calculation threads without
 *       synchronization overhead.
 *       </p>
 */

public final class CreditHistoryRecord {

    /** @docs The contractual date the payment was originally due. */
    private final Date dueDate;

    /**
     * @docs The actual date the payment was settled. Can be null if the transaction
     *       is defaulted or pending.
     */
    private final Date settlementDate;

    /** @docs The structural financial category of this financial record. */
    private final TransactionType transactionType;

    /** @docs The total monetary balance or limit allocated to this transaction. */
    private final double amount;

    /**
     * @docs The current repayment or operational status of this history element.
     */
    private final TransactionStatus status;

    /**
     * @docs Constructs a validated, unmodifiable historical transaction record.
     * @param dueDate         The contractual due date; cannot be null.
     * @param settlementDate  The actual settlement date; can be null if unpaid.
     * @param transactionType The financial mechanism category; cannot be null.
     * @param amount          The monetary sum involved; must be non-negative.
     * @param status          The settlement status; cannot be null.
     * @throws IllegalArgumentException if required parameters are missing or amount
     *                                  is negative.
     */
    public CreditHistoryRecord(Date dueDate, Date settlementDate, TransactionType transactionType, double amount,
            TransactionStatus status) {
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
            throw new IllegalArgumentException("Historical financial transaction amount cannot be negative.");
        }

        this.dueDate = new Date(dueDate.getTime());
        this.settlementDate = (settlementDate != null) ? new Date(settlementDate.getTime()) : null;
        this.transactionType = transactionType;
        this.amount = amount;
        this.status = status;
    }

    /**
     * @docs Retrieves the contractual due date safely.
     * @return A deep copy of the underlying java.util.Date instance.
     */
    public Date getDueDate() {
        return new Date(this.dueDate.getTime());
    }

    /**
     * @docs Retrieves the actual settlement date safely.
     * @return A deep copy of the underlying java.util.Date instance, or null if
     *         never settled.
     */
    public Date getSettlementDate() {
        return (this.settlementDate != null) ? new Date(this.settlementDate.getTime()) : null;
    }

    /**
     * @docs Gets the categorized type of credit used.
     * @return The specific TransactionType enum constant.
     */
    public TransactionType getTransactionType() {
        return this.transactionType;
    }

    /**
     * @docs Retrieves the total value associated with the record.
     * @return The double primitive representing financial volume.
     */
    public double getAmount() {
        return this.amount;
    }

    /**
     * @docs Gets the complete status configuration of this transaction.
     * @return The specific TransactionStatus enum constant.
     */
    public TransactionStatus getStatus() {
        return this.status;
    }

    /**
     * @docs Utility evaluation flag indicating if this specific record counts as a
     *       default breach.
     * @return true if the status equals TransactionStatus.DEFAULTED, false
     *         otherwise.
     */
    public boolean isDefaulted() {
        return this.status == TransactionStatus.DEFAULTED;
    }
}