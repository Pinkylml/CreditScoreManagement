package com.montran.creditscore.domain.model;

/**
 * @docs Indicates the resolution state of a completed or ongoing credit contract milestone.
 * Used directly by core formulas to penalize scores or evaluate reliability.
 */

public enum TransactionStatus {
    /** @docs The obligation was successfully fulfilled on time or within terms. */
    PAID,

    /** @docs The borrower failed to meet the repayment terms, incurring a critical
     *  negative score penalty. */
    DEFAULTED
}
