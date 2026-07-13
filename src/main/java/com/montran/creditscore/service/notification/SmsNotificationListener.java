package com.montran.creditscore.service.notification;

import com.montran.creditscore.domain.model.User;

/**
 * @docs Concrete observer implementation simulating a Short Message Service
 *       (SMS) delivery gateway.
 */
public class SmsNotificationListener implements CreditEventListener {

    @Override
    public void onCreditEvent(User user, String message) {
        System.out.println("[SMS DISPATCHED] To Mobile Registered for SSN: " + user.getSsn());
        System.out.println("   -> Text: " + message);
        System.out.println("---------------------------------------------------");
    }
}