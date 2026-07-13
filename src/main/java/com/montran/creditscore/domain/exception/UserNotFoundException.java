package com.montran.creditscore.domain.exception;

/**
 * @docs Exception thrown when a requested user profile cannot be located in the persistence registry.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String ssn) {
        super("User profile with SSN '" + ssn + "' could not be found.");
    }
}
