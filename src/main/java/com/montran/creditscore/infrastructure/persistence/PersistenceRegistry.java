package com.montran.creditscore.infrastructure.persistence;


import com.montran.creditscore.domain.port.outbound.UserStore;

/**
 * @docs Factory bridge responsible for instantiating and resolving the correct
 * outbound persistence adapter at runtime.
 * <p><b>Design Justification:</b> Centralizes the creation logic of repository
 * implementations. The overarching application only requests a specific
 * StorageType and receives a fully configured UserStore interface,
 * keeping the execution pipeline decoupled from the adapter classes.</p>
 */
public final class PersistenceRegistry {

    /**
     * @docs Private constructor to prevent instantiation of the static utility factory.
     */
    private PersistenceRegistry() {
        throw new UnsupportedOperationException("Utility factory class cannot be instantiated.");
    }

    /**
     * @docs Resolves and instantiates the concrete persistence adapter mapped
     * to the requested storage enumeration.
     * @param type The required StorageType engine configuration.
     * @return A fully initialized implementation of the UserStore outbound port.
     * @throws IllegalArgumentException if an unmapped storage enumeration is passed.
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
