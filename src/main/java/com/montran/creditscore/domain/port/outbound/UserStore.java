package com.montran.creditscore.domain.port.outbound;

import com.montran.creditscore.domain.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port defining persistence operations for User aggregates.
 * The domain layer depends only on this interface, keeping it decoupled
 * from any specific storage technology (XML, JSON, SQL, etc.).
 */
public interface UserStore {

    /** Saves or fully replaces a user record in the store. */
    void save(User user);

    /** Looks up a user by SSN. Returns empty if not found. */
    Optional<User> findBySsn(String ssn);

    /** Returns all users currently in the store. */
    List<User> findAll();

    /**
     * Deletes a user by SSN.
     *
     * @return {@code true} if the user was found and deleted, {@code false} if not found.
     */
    boolean deleteBySsn(String ssn);
}
