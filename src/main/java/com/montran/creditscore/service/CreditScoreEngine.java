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

import java.util.Optional;

/**
 * @docs Primary orchestration service acting as the central use case
 *       interactor.
 *       <p>
 *       <b>Architecture:</b> Implements the Facade Pattern to provide a unified
 *       interface over the subsystems of calculation, risk classification,
 *       persistence, and event broadcasting. This keeps the client application
 *       decoupled from the underlying domain complexity.
 *       </p>
 */
public class CreditScoreEngine {

    private final UserStore userStore;
    private final NotificationSender notificationSender;
    private final ScoreFormula scoreFormula;

    /**
     * @docs Constructs the engine with required outbound ports and assigns the
     *       default mathematical strategy.
     * @param userStore          The persistence port for managing aggregate states.
     * @param notificationSender The messaging port for broadcasting critical
     *                           business events.
     */
    public CreditScoreEngine(UserStore userStore, NotificationSender notificationSender) {
        if (userStore == null || notificationSender == null) {
            throw new IllegalArgumentException("UserStore and NotificationSender ports must be provided.");
        }
        this.userStore = userStore;
        this.notificationSender = notificationSender;
        this.scoreFormula = new WeightedScoreFormula();
    }

    /**
     * @docs Registers a new client profile into the system boundary.
     * @param user The constructed aggregate root to persist.
     * @throws IllegalArgumentException if a user with the same SSN already exists.
     */
    public void registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot register a null user profile.");
        }

        Optional<User> existingUser = userStore.findBySsn(user.getSsn());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User with SSN " + user.getSsn() + " is already registered.");
        }

        userStore.save(user);
    }

    /**
     * @docs Appends a new financial transaction to a specific user's history
     *       ledger.
     * @param ssn    The unique identifier of the target user.
     * @param record The immutable transaction record to append.
     * @throws IllegalArgumentException if the requested user is not found.
     */
    public void addTransaction(String ssn, CreditHistoryRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot append a null transaction record.");
        }

        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new IllegalArgumentException("User with SSN " + ssn + " not found."));

        user.addCreditRecord(record);
        userStore.save(user);
    }

    /**
     * @docs Executes the comprehensive credit rating evaluation, classifies the
     *       resulting risk, updates persistence, and triggers notifications if
     *       critical thresholds are crossed.
     * @param ssn The unique identifier of the target user to evaluate.
     * @throws IllegalArgumentException if the requested user is not found.
     */
    public void evaluateProfile(String ssn) {
        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new IllegalArgumentException("User with SSN " + ssn + " not found."));

        // Capture previous state to evaluate operational deltas
        double previousScore = user.getCreditScore();
        RiskLevel previousRisk = user.getRiskLevel();

        // Load dynamic environmental weights
        ScoreConfiguration config = PropertyWeightLoader.loadWeights();

        // Execute mathematical scoring strategy
        double newScore = scoreFormula.calculate(user, config);
        user.setCreditScore(newScore);

        // Classify operational risk bounds
        RiskLevel newRisk = RiskClassifier.classify(newScore);
        user.setRiskLevel(newRisk);

        // Commit updated state to physical storage
        userStore.save(user);

        // Evaluate state deltas for notification dispatch
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

    /**
     * @docs Removes a user profile completely from the persistence registry.
     * @param ssn The unique identifier of the target user.
     */
    public void deleteUser(String ssn) {
        boolean deleted = userStore.deleteBySsn(ssn);
        if (!deleted) {
            throw new IllegalArgumentException("User with SSN " + ssn + " could not be found for deletion.");
        }
    }

    /**
     * @docs Updates the basic profile information for an existing user.
     * @param ssn        The unique identifier of the user to edit.
     * @param newName    The updated name.
     * @param newAddress The updated address.
     * @param newEmail   The updated email.
     */
    public void editUser(String ssn, String newName, String newAddress, String newEmail) {
        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new IllegalArgumentException("User with SSN " + ssn + " not found."));
        user.updateProfile(newName, newAddress, newEmail);
        userStore.save(user);
    }

    /**
     * @docs Removes a specific transaction from a user's credit history.
     * @param ssn           The unique identifier of the user.
     * @param transactionId The ID of the transaction to delete.
     */
    public void deleteTransaction(String ssn, String transactionId) {
        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new IllegalArgumentException("User with SSN " + ssn + " not found."));
        boolean removed = user.removeCreditRecord(transactionId);
        if (removed) {
            userStore.save(user);
        }
    }

    /**
     * @docs Replaces an existing transaction in a user's credit history with new
     *       data.
     * @param ssn           The unique identifier of the user.
     * @param transactionId The ID of the transaction to update.
     * @param updatedRecord The new transaction record data.
     */
    public void editTransaction(String ssn, String transactionId, CreditHistoryRecord updatedRecord) {
        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new IllegalArgumentException("User with SSN " + ssn + " not found."));
        boolean updated = user.updateCreditRecord(transactionId, updatedRecord);
        if (updated) {
            userStore.save(user);
        }
    }
}