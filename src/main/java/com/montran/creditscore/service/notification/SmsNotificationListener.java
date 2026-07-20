package com.montran.creditscore.service.notification;

import com.montran.creditscore.domain.model.User;

/**
 * Simulates SMS delivery for credit profile change events.
 * In a production system this would integrate a real SMS gateway (e.g., Twilio).
 * Registered with {@link CreditEventPublisher} and invoked asynchronously
 * whenever a significant score or risk change is detected.
 */
public class SmsNotificationListener implements CreditEventListener {

    /**
     * Prints a simulated SMS to stdout.
     *
     * @param user    The user whose credit profile changed. Never null.
     * @param message A human-readable description of the change.
     */
    @Override
    public void onCreditEvent(User user, String message) {
        System.out.println("[SMS DISPATCHED] To Mobile Registered for SSN: " + user.getSsn());
        System.out.println("   -> Text: " + message);
        System.out.println("---------------------------------------------------");
    }
}