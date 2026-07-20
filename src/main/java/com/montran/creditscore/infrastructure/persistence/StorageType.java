package com.montran.creditscore.infrastructure.persistence;

/**
 * Enumerates the supported file-based storage formats for the persistence layer.
 * The active format is resolved at startup from {@code credit-settings.properties}
 * by {@link com.montran.creditscore.infrastructure.config.PropertyWeightLoader} and
 * passed to {@link PersistenceRegistry#getStore(StorageType)} to obtain the correct
 * {@link com.montran.creditscore.domain.port.outbound.UserStore} implementation.
 */
public enum StorageType {
    /** Users are serialized to {@code users.xml} using JAXB. */
    XML,
    /** Users are serialized to {@code users.json} using Gson. */
    JSON
}
