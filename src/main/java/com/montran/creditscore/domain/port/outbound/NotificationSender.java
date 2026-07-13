package com.montran.creditscore.domain.port.outbound;

import com.montran.creditscore.domain.model.User;

/**
 * @docs Outbound port abstraction governing communications and alert management.
 * <p><b>Design Justification:</b> Enforces the Single Responsibility Principle (SRP)
 * by ensuring that credit scoring routines remain completely separated from notification
 * channels (e.g., mail templates, network communication protocols, or SMS systems).</p>
 */
public interface NotificationSender {

    /**
     * @docs Transmits an alert message to a user informing them of significant
     * changes to their profile.
     * @param user The User aggregate root instance whose profile triggered
     * the modification event.
     * @param message Description text summarizing the score or risk alteration parameters.
     */
    void sendNotification(User user, String message);
}