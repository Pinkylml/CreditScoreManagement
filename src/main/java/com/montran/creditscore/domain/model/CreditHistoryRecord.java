package com.montran.creditscore.domain.model;

import java.util.Date;
import java.util.UUID;
import com.montran.creditscore.domain.exception.InvalidCreditDataException;

/**
 * @docs A Domain Value Object representing an immutable financial transaction
 *       entry.
 */
public final class CreditHistoryRecord {

    /** @docs The unique identifier for this specific transaction record. */
    private final String transactionId;

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
     * @param transactionId   The unique ID. If null, a new UUID is automatically
     *                        generated.
     * @param dueDate         The contractual due date; cannot be null.
     * @param settlementDate  The actual settlement date; can be null if unpaid.
     * @param transactionType The financial mechanism category; cannot be null.
     * @param amount          The monetary sum involved; must be non-negative.
     * @param status          The settlement status; cannot be null.
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