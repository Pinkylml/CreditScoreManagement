package com.montran.creditscore.service.notification;

import com.montran.creditscore.domain.model.User;

/** Simulates email delivery for credit event notifications. */
public class EmailNotificationListener implements CreditEventListener {

    @Override
    public void onCreditEvent(User user, String message) {
        System.out.println("[EMAIL DISPATCHED] To: " + user.getEmail() + " | Account: " + user.getName());
        System.out.println("   -> Subject: Important Update Regarding Your Credit Profile");
        System.out.println("   -> Body: " + message);
        System.out.println("---------------------------------------------------");
    }
}