package com.montran.creditscore.service.notification;

import com.montran.creditscore.domain.model.User;

/**
 * Observer contract for receiving credit profile change events.
 * Implement this interface to plug in a new notification channel.
 */
public interface CreditEventListener {

    /**
     * Called when a significant change occurs in a user's credit profile.
     *
     * @param user    The user whose profile changed.
     * @param message A description of what changed (score delta or risk shift).
     */
    void onCreditEvent(User user, String message);
}