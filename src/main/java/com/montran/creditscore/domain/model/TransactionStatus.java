package com.montran.creditscore.domain.model;

/**
 * Indicates whether a transaction obligation has been met or failed.
 * The scoring formula treats DEFAULTED records as missed payments, skipping
 * them in the on-time payment count.
 */
public enum TransactionStatus {
    PAID,
    DEFAULTED
}
