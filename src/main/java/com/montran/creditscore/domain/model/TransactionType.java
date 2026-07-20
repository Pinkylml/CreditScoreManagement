package com.montran.creditscore.domain.model;

/** Supported credit instrument categories used in diversity scoring. */
public enum TransactionType {
    CREDIT_CARD,
    MORTGAGE,
    AUTO_LOAN,
    STUDENT_LOAN,
    LOAN,
    /** Represents a hard credit inquiry; used exclusively by the inquiry-penalty calculation. */
    INQUIRY
}