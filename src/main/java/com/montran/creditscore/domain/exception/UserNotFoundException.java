package com.montran.creditscore.domain.exception;

/** Thrown when a lookup by SSN finds no matching user in the store. */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String ssn) {
        super("User profile with SSN '" + ssn + "' could not be found.");
    }
}
