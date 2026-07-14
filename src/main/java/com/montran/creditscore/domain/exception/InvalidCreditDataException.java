package com.montran.creditscore.domain.exception;

/** Thrown when a domain constraint is violated during data creation (e.g., negative credit limit). */
public class InvalidCreditDataException extends RuntimeException {
    public InvalidCreditDataException(String message) {
        super(message);
    }
}
