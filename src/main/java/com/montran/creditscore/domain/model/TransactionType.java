package com.montran.creditscore.domain.model;

/**
 * Classifies a credit instrument by its category.
 *
 * <p>The scoring formula uses these values for two distinct purposes:</p>
 * <ul>
 *   <li><strong>Diversity scoring</strong> – counts how many distinct non-{@link #INQUIRY}
 *       types appear in a user's history (each unique type contributes 2 points).</li>
 *   <li><strong>Inquiry penalty</strong> – only {@link #INQUIRY} records within the last
 *       24 months are counted; each deducts 2 points from the final score.</li>
 * </ul>
 */
public enum TransactionType {
    CREDIT_CARD,
    MORTGAGE,
    AUTO_LOAN,
    STUDENT_LOAN,
    LOAN,
    /**
     * Represents a hard credit inquiry.
     * Excluded from utilization, payment history, credit age, and diversity scoring.
     * Counted exclusively by the inquiry-penalty component of the formula.
     */
    INQUIRY
}