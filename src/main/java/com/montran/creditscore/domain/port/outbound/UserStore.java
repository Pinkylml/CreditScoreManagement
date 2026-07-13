package com.montran.creditscore.domain.port.outbound;

import com.montran.creditscore.domain.model.User;
import java.util.List;
import java.util.Optional;

/**
 * @docs Outbound Service Provider Interface (SPI) defining core
 * persistence operations for User aggregates.
 * <p><b>Design Justification:</b> Decouples the business and logic layers
 * from physical data engines (XML, JSON, or future database drivers).
 * The system interacts solely with this abstraction.</p>
 */

public interface UserStore {
    /**
     * @docs Persists a new user record or completely replaces an existing
     * one within the storage engine.
     * @param user The non-null User aggregate root instance to persist.
     */
    void save(User user);

    /**
     * @docs Performs a primary key lookup using the unique Social Security Number (SSN).
     * @param ssn The unique identity token of the user.
     * @return An Optional containing the populated User domain aggregate root, or empty
     * if not located.
     */
    Optional<User> findBySsn(String ssn);

    /**
     * @docs Obtains a complete collection of all users managed inside the storage medium.
     * @return A List containing all active User aggregate instances.
     */
    List<User> findAll();

    /**
     * @docs Permanently deletes a targeted user profile from the infrastructure storage
     * file using their primary key.
     * @param ssn The unique Social Security Number of the user to be expunged.
     * @return true if the execution successfully removed the match, false if the
     * profile was not found.
     */
    boolean deleteBySsn(String ssn);
}
