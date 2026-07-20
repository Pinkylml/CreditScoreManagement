package com.montran.creditscore.service;

import com.montran.creditscore.domain.model.CreditHistoryRecord;
import com.montran.creditscore.domain.model.RiskLevel;
import com.montran.creditscore.domain.model.ScoreConfiguration;
import com.montran.creditscore.domain.model.User;
import com.montran.creditscore.domain.port.outbound.NotificationSender;
import com.montran.creditscore.domain.port.outbound.UserStore;
import com.montran.creditscore.infrastructure.config.PropertyWeightLoader;
import com.montran.creditscore.service.calculation.ScoreFormula;
import com.montran.creditscore.service.calculation.WeightedScoreFormula;
import com.montran.creditscore.service.risk.RiskClassifier;
import com.montran.creditscore.domain.exception.UserNotFoundException;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Facade that exposes all credit management use cases to the outside world.
 * Coordinates user registration, transaction recording, score evaluation,
 * risk classification, persistence, and event notifications.
 *
 * <h3>Thread Safety</h3>
 * <p>All state-mutating operations are protected by a per-user {@link ReentrantLock} keyed
 * on the SSN. This allows full concurrency between independent users while serialising
 * concurrent writes to the same user. The pattern is:</p>
 * <ol>
 *   <li>Acquire {@code userLocks.computeIfAbsent(ssn, ...)} lock.</li>
 *   <li>Inside a {@code try/finally}: fetch (defensive copy from store), mutate, save.</li>
 *   <li>Release lock in {@code finally}.</li>
 * </ol>
 * <p>The store itself returns defensive copies on every read, so the engine works on an
 * independent snapshot; no external caller can mutate the cache unless {@code save()} is called.</p>
 */
public class CreditScoreEngine {

    private final UserStore userStore;
    private final NotificationSender notificationSender;
    private final ScoreFormula scoreFormula;

    /**
     * Per-user locks that serialise concurrent write operations on the same SSN.
     * Entries are added lazily on first access and are never removed (lock instances are cheap
     * and their lifetime matches the engine instance).
     */
    private final ConcurrentHashMap<String, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    /**
     * Builds the engine with the required persistence and notification ports.
     * Defaults to {@link WeightedScoreFormula} for score calculation.
     *
     * @param userStore          Where users are stored and retrieved.
     * @param notificationSender How credit alerts are delivered.
     * @throws IllegalArgumentException if either port is null.
     */
    public CreditScoreEngine(UserStore userStore, NotificationSender notificationSender) {
        if (userStore == null || notificationSender == null) {
            throw new IllegalArgumentException("UserStore and NotificationSender ports must be provided.");
        }
        this.userStore = userStore;
        this.notificationSender = notificationSender;
        this.scoreFormula = new WeightedScoreFormula();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the canonical {@link ReentrantLock} for the given SSN, creating it atomically
     * if it does not yet exist.
     */
    private ReentrantLock lockFor(String ssn) {
        return userLocks.computeIfAbsent(ssn, key -> new ReentrantLock());
    }

    /**
     * Recalculates the credit score and risk level for a user, saves the updated state,
     * and fires a notification if the risk level changed or the score shifted by ≥10 points.
     * Must be called while holding the per-user lock.
     */
    private void recalculateAndPersist(User user) {
        double previousScore = user.getCreditScore();
        RiskLevel previousRisk = user.getRiskLevel();

        ScoreConfiguration config = PropertyWeightLoader.loadWeights();
        double newScore = scoreFormula.calculate(user, config);
        user.setCreditScore(newScore);

        RiskLevel newRisk = RiskClassifier.classify(newScore,
                config.getRiskThresholdLow(), config.getRiskThresholdMedium());
        user.setRiskLevel(newRisk);

        userStore.save(user);

        if (previousRisk != newRisk) {
            String alert = String.format(
                    "Your credit risk level has shifted from %s to %s. Your new operational score is %.2f.",
                    previousRisk, newRisk, newScore);
            notificationSender.sendNotification(user, alert);
        } else if (Math.abs(previousScore - newScore) >= 10.0) {
            String alert = String.format(
                    "We detected a significant shift in your credit profile. Your new operational score is %.2f.",
                    newScore);
            notificationSender.sendNotification(user, alert);
        }
    }

    // -----------------------------------------------------------------------
    // User lifecycle
    // -----------------------------------------------------------------------

    /**
     * Registers a new user. Fails if a user with the same SSN already exists.
     *
     * @param user The user to register. Cannot be null.
     * @throws IllegalArgumentException if user is null or the SSN is already taken.
     */
    public void registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot register a null user profile.");
        }

        ReentrantLock lock = lockFor(user.getSsn());
        lock.lock();
        try {
            Optional<User> existingUser = userStore.findBySsn(user.getSsn());
            if (existingUser.isPresent()) {
                throw new IllegalArgumentException(
                        "User with SSN " + user.getSsn() + " is already registered.");
            }
            userStore.save(user);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Permanently removes a user from the store.
     *
     * @param ssn The user's SSN.
     * @throws IllegalArgumentException if the user is not found.
     */
    public void deleteUser(String ssn) {
        ReentrantLock lock = lockFor(ssn);
        lock.lock();
        try {
            boolean deleted = userStore.deleteBySsn(ssn);
            if (!deleted) {
                throw new IllegalArgumentException(
                        "User with SSN " + ssn + " could not be found for deletion.");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates a user's name, address, and email. Null or blank values are ignored.
     * The change is persisted atomically under the user's lock.
     *
     * @param ssn        The user's SSN.
     * @param newName    Updated name.
     * @param newAddress Updated address.
     * @param newEmail   Updated email.
     * @throws UserNotFoundException if the user is not found.
     */
    public void editUser(String ssn, String newName, String newAddress, String newEmail) {
        ReentrantLock lock = lockFor(ssn);
        lock.lock();
        try {
            User user = userStore.findBySsn(ssn)
                    .orElseThrow(() -> new UserNotFoundException(ssn));
            user.updateProfile(newName, newAddress, newEmail);
            userStore.save(user);
        } finally {
            lock.unlock();
        }
    }

    // -----------------------------------------------------------------------
    // Score evaluation
    // -----------------------------------------------------------------------

    /**
     * Runs a full credit evaluation for a user: calculates the score, assigns a risk level,
     * saves the result, and fires a notification if the risk level changed or the score
     * shifted by 10+ points.
     *
     * @param ssn The user's SSN.
     * @throws UserNotFoundException if the user is not found.
     */
    public void evaluateProfile(String ssn) {
        ReentrantLock lock = lockFor(ssn);
        lock.lock();
        try {
            User user = userStore.findBySsn(ssn)
                    .orElseThrow(() -> new UserNotFoundException(ssn));
            recalculateAndPersist(user);
        } finally {
            lock.unlock();
        }
    }

    // -----------------------------------------------------------------------
    // Transaction management  (atomic read-modify-write under per-user lock)
    // -----------------------------------------------------------------------

    /**
     * Atomically appends a transaction to a user's credit history, then re-evaluates
     * the credit score and risk level under the user's exclusive lock.
     *
     * <p>Lifecycle under lock:
     * <ol>
     *   <li>Fetch a defensive copy from the store.</li>
     *   <li>Add the record to the copy.</li>
     *   <li>Recalculate score + risk and persist.</li>
     * </ol></p>
     *
     * @param ssn    The user's SSN.
     * @param record The transaction to add. Cannot be null.
     * @throws UserNotFoundException    if the user is not found.
     * @throws IllegalArgumentException if record is null.
     */
    public void addTransactionToUser(String ssn, CreditHistoryRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot append a null transaction record.");
        }

        ReentrantLock lock = lockFor(ssn);
        lock.lock();
        try {
            User user = userStore.findBySsn(ssn)
                    .orElseThrow(() -> new UserNotFoundException(ssn));
            user.addCreditRecord(record);
            recalculateAndPersist(user);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Backward-compatible alias for {@link #addTransactionToUser(String, CreditHistoryRecord)}.
     * Kept so that existing call sites compile without changes.
     *
     * @param ssn    The user's SSN.
     * @param record The transaction to add. Cannot be null.
     */
    public void addTransaction(String ssn, CreditHistoryRecord record) {
        addTransactionToUser(ssn, record);
    }

    /**
     * Atomically replaces a specific transaction in a user's credit history, then
     * re-evaluates the credit score and risk level under the user's exclusive lock.
     *
     * @param ssn           The user's SSN.
     * @param transactionId The ID of the transaction to replace.
     * @param updatedRecord The new transaction data.
     * @throws UserNotFoundException if the user is not found.
     */
    public void updateTransactionForUser(String ssn, String transactionId,
            CreditHistoryRecord updatedRecord) {
        ReentrantLock lock = lockFor(ssn);
        lock.lock();
        try {
            User user = userStore.findBySsn(ssn)
                    .orElseThrow(() -> new UserNotFoundException(ssn));
            boolean updated = user.updateCreditRecord(transactionId, updatedRecord);
            if (updated) {
                recalculateAndPersist(user);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Backward-compatible alias for
     * {@link #updateTransactionForUser(String, String, CreditHistoryRecord)}.
     *
     * @param ssn           The user's SSN.
     * @param transactionId The ID of the transaction to replace.
     * @param updatedRecord The new transaction data.
     */
    public void editTransaction(String ssn, String transactionId, CreditHistoryRecord updatedRecord) {
        updateTransactionForUser(ssn, transactionId, updatedRecord);
    }

    /**
     * Atomically removes a specific transaction from a user's credit history and
     * re-evaluates the credit score under the user's exclusive lock.
     *
     * @param ssn           The user's SSN.
     * @param transactionId The ID of the transaction to remove.
     * @throws UserNotFoundException if the user is not found.
     */
    public void deleteTransaction(String ssn, String transactionId) {
        ReentrantLock lock = lockFor(ssn);
        lock.lock();
        try {
            User user = userStore.findBySsn(ssn)
                    .orElseThrow(() -> new UserNotFoundException(ssn));
            boolean removed = user.removeCreditRecord(transactionId);
            if (removed) {
                recalculateAndPersist(user);
            }
        } finally {
            lock.unlock();
        }
    }
}