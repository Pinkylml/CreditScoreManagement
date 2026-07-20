package com.montran.creditscore.domain.exception;

/**
 * Thrown when a persistence operation (read or write) fails due to an I/O,
 * serialization, or deserialization error.
 *
 * <p>Infrastructure exceptions ({@code IOException}, {@code JAXBException},
 * {@code JsonSyntaxException}, etc.) must never leak out of the persistence layer;
 * they are always wrapped in this exception so callers depend only on the domain
 * exception hierarchy, not on specific storage technology.</p>
 */
public class PersistenceException extends RuntimeException {

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
