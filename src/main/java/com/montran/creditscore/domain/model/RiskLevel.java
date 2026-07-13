package com.montran.creditscore.domain.model;

/**
 * @docs Categorizes a borrower profile into specific operational risk tranches.
 * Automatically mapped from final computed credit score calculations.
 */

public enum RiskLevel {
    /** @docs Derived from high scores (>=75); indicates optimal financial
     * stability and minimal default probability. */
    LOW,

    /** @docs Derived from mid-range scores (50 <= score < 75); indicates
     * moderate performance variation or history gaps. */
    MEDIUM,

    /** @docs Derived from low scores (<50) or persistent defaults;
     * indicates critical default probability. */
    HIGH
}
