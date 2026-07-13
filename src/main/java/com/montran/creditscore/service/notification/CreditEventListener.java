package com.montran.creditscore.service.notification;

import com.montran.creditscore.domain.model.User;

/**
 * @docs Observer interface defining the contract for credit event listeners.
 *       <p>
 *       <b>Design Justification:</b> Enforces the Observer Pattern. This allows
 *       multiple distinct notification channels to be plugged into the system
 *       dynamically without tightly coupling the delivery mechanism to the
 *       publisher.
 *       </p>
 */
public interface CreditEventListener {

    /**
     * @docs Handles the dispatched event asynchronously when a significant user
     *       profile change occurs.
     * @param user    The domain aggregate root whose profile triggered the
     *                notification.
     * @param message The alert message detailing the risk or score modification.
     */
    void onCreditEvent(User user, String message);
}