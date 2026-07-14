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

/**
 * Facade that exposes all credit management use cases to the outside world.
 * Coordinates user registration, transaction recording, score evaluation,
 * risk classification, persistence, and event notifications.
 */
public class CreditScoreEngine {

    private final UserStore userStore;
    private final NotificationSender notificationSender;
    private final ScoreFormula scoreFormula;

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

        Optional<User> existingUser = userStore.findBySsn(user.getSsn());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User with SSN " + user.getSsn() + " is already registered.");
        }

        userStore.save(user);
    }

    /**
     * Adds a transaction to a user's credit history and persists the change.
     *
     * @param ssn    The user's SSN.
     * @param record The transaction to add. Cannot be null.
     * @throws UserNotFoundException     if the user is not found.
     * @throws IllegalArgumentException  if record is null.
     */
    public void addTransaction(String ssn, CreditHistoryRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Cannot append a null transaction record.");
        }

        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new UserNotFoundException(ssn));

        user.addCreditRecord(record);
        userStore.save(user);
    }

    /**
     * Runs a full credit evaluation for a user: calculates the score, assigns a risk level,
     * saves the result, and fires a notification if the risk level changed or the score
     * shifted by 10+ points.
     *
     * @param ssn The user's SSN.
     * @throws UserNotFoundException if the user is not found.
     */
    public void evaluateProfile(String ssn) {
        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new UserNotFoundException(ssn));

        double previousScore = user.getCreditScore();
        RiskLevel previousRisk = user.getRiskLevel();

        ScoreConfiguration config = PropertyWeightLoader.loadWeights();

        double newScore = scoreFormula.calculate(user, config);
        user.setCreditScore(newScore);

        RiskLevel newRisk = RiskClassifier.classify(newScore, config.getRiskThresholdLow(), config.getRiskThresholdMedium());
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

    /**
     * Permanently removes a user from the store.
     *
     * @param ssn The user's SSN.
     * @throws IllegalArgumentException if the user is not found.
     */
    public void deleteUser(String ssn) {
        boolean deleted = userStore.deleteBySsn(ssn);
        if (!deleted) {
            throw new IllegalArgumentException("User with SSN " + ssn + " could not be found for deletion.");
        }
    }

    /**
     * Updates a user's name, address, and email. Null or blank values are ignored.
     *
     * @param ssn        The user's SSN.
     * @param newName    Updated name.
     * @param newAddress Updated address.
     * @param newEmail   Updated email.
     * @throws UserNotFoundException if the user is not found.
     */
    public void editUser(String ssn, String newName, String newAddress, String newEmail) {
        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new UserNotFoundException(ssn));
        user.updateProfile(newName, newAddress, newEmail);
        userStore.save(user);
    }

    /**
     * Removes a specific transaction from a user's credit history.
     *
     * @param ssn           The user's SSN.
     * @param transactionId The ID of the transaction to remove.
     * @throws UserNotFoundException if the user is not found.
     */
    public void deleteTransaction(String ssn, String transactionId) {
        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new UserNotFoundException(ssn));
        boolean removed = user.removeCreditRecord(transactionId);
        if (removed) {
            userStore.save(user);
        }
    }

    /**
     * Replaces a specific transaction in a user's credit history with updated data.
     *
     * @param ssn           The user's SSN.
     * @param transactionId The ID of the transaction to replace.
     * @param updatedRecord The new transaction data.
     * @throws UserNotFoundException if the user is not found.
     */
    public void editTransaction(String ssn, String transactionId, CreditHistoryRecord updatedRecord) {
        User user = userStore.findBySsn(ssn)
                .orElseThrow(() -> new UserNotFoundException(ssn));
        boolean updated = user.updateCreditRecord(transactionId, updatedRecord);
        if (updated) {
            userStore.save(user);
        }
    }
}