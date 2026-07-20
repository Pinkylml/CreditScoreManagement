package com.montran.creditscore.service.notification;

import com.montran.creditscore.domain.model.User;

/**
 * Simulates email delivery for credit profile change events.
 * In a production system this would integrate a real SMTP or email-API client.
 * Registered with {@link com.montran.creditscore.service.notification.CreditEventPublisher}
 * and invoked asynchronously whenever a significant score or risk change is detected.
 */
public class EmailNotificationListener implements CreditEventListener {

    /**
     * Prints a simulated email to stdout. Non-blocking in production as it is called
     * from a {@link java.util.concurrent.CompletableFuture} inside the publisher.
     *
     * @param user    The user whose credit profile changed. Never null.
     * @param message A human-readable description of the change.
     */
    @Override
    public void onCreditEvent(User user, String message) {
        System.out.println("[EMAIL DISPATCHED] To: " + user.getEmail() + " | Account: " + user.getName());
        System.out.println("   -> Subject: Important Update Regarding Your Credit Profile");
        System.out.println("   -> Body: " + message);
        System.out.println("---------------------------------------------------");
    }
}