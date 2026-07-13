package com.montran.creditscore.domain.exception;

/**
 * @docs Exception thrown when domain constraints or business invariants are violated during data instantiation.
 */
public class InvalidCreditDataException extends RuntimeException {
    public InvalidCreditDataException(String message) {
        super(message);
    }
}
