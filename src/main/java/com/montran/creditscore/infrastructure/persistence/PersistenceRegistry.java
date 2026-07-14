package com.montran.creditscore.infrastructure.persistence;

import com.montran.creditscore.domain.port.outbound.UserStore;

/**
 * Factory that returns the correct {@link UserStore} implementation based on the configured storage type.
 * The active type is read from {@code credit-settings.properties} at startup.
 */
public final class PersistenceRegistry {

    private PersistenceRegistry() {
        throw new UnsupportedOperationException("Utility factory class cannot be instantiated.");
    }

    /**
     * Returns a fully initialized store for the given storage type.
     *
     * @param type The desired storage engine (XML or JSON).
     * @return A ready-to-use {@link UserStore} implementation.
     * @throws IllegalArgumentException if type is null or unsupported.
     */
    public static UserStore getStore(StorageType type) {
        if (type == null) {
            throw new IllegalArgumentException("Storage type configuration cannot be null.");
        }
        switch (type) {
            case XML:
                return new XmlUserStore();
            case JSON:
                return new JsonUserStore();
            default:
                throw new IllegalArgumentException("Unsupported storage framework requested: " + type);
        }
    }
}
