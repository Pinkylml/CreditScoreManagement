package com.montran.creditscore.domain.port.outbound;

import com.montran.creditscore.domain.model.User;

/**
 * Outbound port for sending credit event alerts to users.
 * Concrete implementations handle the actual delivery channel (email, SMS, etc.).
 */
public interface NotificationSender {

    /**
     * Sends an alert message to a user about a significant change to their credit profile.
     *
     * @param user    The user whose profile triggered the event.
     * @param message A human-readable description of the change.
     */
    void sendNotification(User user, String message);
}