package com.montran.creditscore.domain.exception;

/**
 * Thrown when an attempt is made to register a user whose SSN already exists in the store.
 * Replaces the generic {@link IllegalArgumentException} that was used for this purpose.
 */
public class DuplicateUserException extends RuntimeException {

    private final String ssn;

    public DuplicateUserException(String ssn) {
        super("A user with SSN '" + ssn + "' is already registered in the system.");
        this.ssn = ssn;
    }

    /** Returns the SSN that caused the conflict. */
    public String getSsn() {
        return ssn;
    }
}
