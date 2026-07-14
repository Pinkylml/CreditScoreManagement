package com.montran.creditscore.domain.model;

/**
 * Classifies a user's credit risk based on their score.
 * Automatically mapped by {@link com.montran.creditscore.service.risk.RiskClassifier}
 * after each evaluation.
 */
public enum RiskLevel {
    /** Score >= 75. Low default probability. */
    LOW,

    /** 50 <= score < 75. Some history gaps or moderate payment issues. */
    MEDIUM,

    /** Score < 50. High default probability. */
    HIGH
}
