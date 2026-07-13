package com.montran.creditscore.infrastructure.persistence;

import com.montran.creditscore.domain.model.CreditHistoryRecord;
import com.montran.creditscore.domain.model.RiskLevel;
import com.montran.creditscore.domain.model.TransactionStatus;
import com.montran.creditscore.domain.model.TransactionType;
import com.montran.creditscore.domain.model.User;
import com.montran.creditscore.domain.port.outbound.UserStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integrates and tests the persistence infrastructure layer,
 * validating physical I/O and data translation mapping.
 */
class PersistenceIntegrationTest {

    private static final String XML_FILE = "users.xml";
    private static final String JSON_FILE = "users.json";

    private User testUser;

    @BeforeEach
    void setUp() {
        // Initialize a pure Domain Aggregate
        testUser = new User("111-22-3333", "Jefferson Cando", "Quito, Ecuador", "jefferson@montran.com", 10000.0);
        testUser.setCreditScore(85.5);
        testUser.setRiskLevel(RiskLevel.LOW);

        // Attach an immutable historical record
        CreditHistoryRecord record = new CreditHistoryRecord(
                new Date(),
                new Date(),
                TransactionType.CREDIT_CARD,
                5000.00,
                TransactionStatus.PAID);
        testUser.addCreditRecord(record);
    }

    @AfterEach
    void tearDown() {
        // Clean up structural database files after each execution to avoid state
        // contamination
        new File(XML_FILE).delete();
        new File(JSON_FILE).delete();
    }

    @Test
    void shouldResolveCorrectAdapterFromRegistry() {
        UserStore xmlStore = PersistenceRegistry.getStore(StorageType.XML);
        assertTrue(xmlStore instanceof XmlUserStore, "Registry failed to yield the XML adapter.");

        UserStore jsonStore = PersistenceRegistry.getStore(StorageType.JSON);
        assertTrue(jsonStore instanceof JsonUserStore, "Registry failed to yield the JSON adapter.");
    }

    @Test
    void xmlStoreShouldSaveAndRetrieveUserSuccessfully() {
        UserStore store = PersistenceRegistry.getStore(StorageType.XML);

        // Execute physical write
        store.save(testUser);

        // Execute physical read via a fresh store instance to bypass in-memory cache
        UserStore freshStore = PersistenceRegistry.getStore(StorageType.XML);
        Optional<User> retrievedOpt = freshStore.findBySsn(testUser.getSsn());

        assertTrue(retrievedOpt.isPresent(), "User was not retrieved from XML store.");
        User retrieved = retrievedOpt.get();

        assertEquals(testUser.getName(), retrieved.getName());
        assertEquals(testUser.getEmail(), retrieved.getEmail());
        assertEquals(testUser.getTotalCreditLimit(), retrieved.getTotalCreditLimit());
        assertEquals(testUser.getCreditScore(), retrieved.getCreditScore());
        assertEquals(1, retrieved.getCreditHistory().size());
        assertEquals(TransactionType.CREDIT_CARD, retrieved.getCreditHistory().get(0).getTransactionType());
    }

    @Test
    void jsonStoreShouldSaveAndRetrieveUserSuccessfully() {
        UserStore store = PersistenceRegistry.getStore(StorageType.JSON);

        // Execute physical write
        store.save(testUser);

        // Execute physical read via a fresh store instance to bypass in-memory cache
        UserStore freshStore = PersistenceRegistry.getStore(StorageType.JSON);
        Optional<User> retrievedOpt = freshStore.findBySsn(testUser.getSsn());

        assertTrue(retrievedOpt.isPresent(), "User was not retrieved from JSON store.");
        User retrieved = retrievedOpt.get();

        assertEquals(testUser.getName(), retrieved.getName());
        assertEquals(testUser.getEmail(), retrieved.getEmail());
        assertEquals(testUser.getTotalCreditLimit(), retrieved.getTotalCreditLimit());
        assertEquals(testUser.getCreditScore(), retrieved.getCreditScore());
        assertEquals(1, retrieved.getCreditHistory().size());
        assertEquals(TransactionStatus.PAID, retrieved.getCreditHistory().get(0).getStatus());
    }

    @Test
    void storeShouldDeleteUserSuccessfully() {
        UserStore store = PersistenceRegistry.getStore(StorageType.JSON);
        store.save(testUser);

        boolean isDeleted = store.deleteBySsn(testUser.getSsn());
        assertTrue(isDeleted, "Deletion routine failed.");

        Optional<User> retrievedOpt = store.findBySsn(testUser.getSsn());
        assertFalse(retrievedOpt.isPresent(), "User should no longer exist in the registry.");
    }
}