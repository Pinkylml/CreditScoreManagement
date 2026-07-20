package com.montran.creditscore.domain.exception;

/**
 * Thrown when the credit-score formula encounters data that makes a calculation
 * mathematically impossible or logically inconsistent (e.g., a null configuration
 * weight, a zero total-payment divisor when payments exist, or a null user aggregate
 * passed to the formula).
 */
public class CreditCalculationException extends RuntimeException {

    public CreditCalculationException(String message) {
        super(message);
    }

    public CreditCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
