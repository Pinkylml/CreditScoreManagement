package com.montran.creditscore;

import com.montran.creditscore.domain.model.CreditHistoryRecord;
import com.montran.creditscore.domain.model.TransactionStatus;
import com.montran.creditscore.domain.model.TransactionType;
import com.montran.creditscore.domain.model.User;
import com.montran.creditscore.domain.port.outbound.UserStore;
import com.montran.creditscore.infrastructure.persistence.PersistenceRegistry;
import com.montran.creditscore.service.CreditScoreEngine;
import com.montran.creditscore.service.notification.CreditEventPublisher;
import com.montran.creditscore.service.notification.EmailNotificationListener;
import com.montran.creditscore.service.notification.SmsNotificationListener;
import com.montran.creditscore.infrastructure.config.PropertyWeightLoader;

import java.util.Calendar;
import java.util.Date;

public class Main {

    public static void main(String[] args) {
        System.out.println("===================================================");
        System.out.println("   INITIALIZING CREDIT SCORE MANAGEMENT SYSTEM     ");
        System.out.println("===================================================\n");

        UserStore store = PersistenceRegistry.getStore(PropertyWeightLoader.loadStorageType());

        CreditEventPublisher publisher = new CreditEventPublisher();
        publisher.subscribe(new EmailNotificationListener());
        publisher.subscribe(new SmsNotificationListener());

        CreditScoreEngine engine = new CreditScoreEngine(store, publisher);

        runScenarioOne(engine);
        runScenarioTwo(engine);
        runScenarioThree(engine);
        runScenarioFour(engine);
        runScenarioFive(engine);

        demonstrateUserManagement(engine);

        System.out.println("\n===================================================");
        System.out.println("               SYSTEM DEMO COMPLETE                ");
        System.out.println("===================================================");
    }

    // Scenario 1 – Excellent history: $10,000 limit, $2,000 used, 20 on-time payments, 5-yr age, 3 types, 1 recent inquiry.
    private static void runScenarioOne(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 1: Excellent Credit History");
        String ssn = "111-22-3333";
        User user = new User(ssn, "Jefferson Cando", "Quito, Ecuador", "jeff@example.com", 10000.0);
        engine.registerUser(user);

        for (int i = 0; i < 20; i++) {
            int yearsAgo = (i == 0) ? 1 : 6;
            double amount = 2000.0 / 20;
            TransactionType type = (i % 3 == 0) ? TransactionType.CREDIT_CARD
                    : (i % 3 == 1) ? TransactionType.MORTGAGE : TransactionType.AUTO_LOAN;

            engine.addTransaction(ssn, createRecord(amount, yearsAgo * 365, 5, type));
        }

        engine.evaluateProfile(ssn);
        System.out.println("Scenario 1 Evaluated.\n");
    }

    // Scenario 2 – Good history with late payments: $20,000 limit, $8,000 used, 13/15 on-time, 8-yr age, 4 types, 0 recent inquiries.
    private static void runScenarioTwo(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 2: Good Credit History with a Few Late Payments");
        String ssn = "222-33-4444";
        User user = new User(ssn, "Alice Smith", "New York, USA", "alice@example.com", 20000.0);
        engine.registerUser(user);

        for (int i = 0; i < 15; i++) {
            int yearsAgo = 8;
            int daysLate = (i < 2) ? 45 : 10;
            double amount = 8000.0 / 15;
            TransactionType type = TransactionType.values()[i % 4];

            engine.addTransaction(ssn, createRecord(amount, yearsAgo * 365, daysLate, type));
        }

        engine.evaluateProfile(ssn);
        System.out.println("Scenario 2 Evaluated.\n");
    }

    // Scenario 3 – High utilization: $5,000 limit, $4,500 used, 9/10 on-time, 3-yr age, 2 types, 3 recent inquiries.
    private static void runScenarioThree(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 3: Moderate Credit History with High Utilization");
        String ssn = "333-44-5555";
        User user = new User(ssn, "Robert Johnson", "London, UK", "robert@example.com", 5000.0);
        engine.registerUser(user);

        for (int i = 0; i < 10; i++) {
            int daysAgo = 3 * 365;
            if (i < 3)
                daysAgo = 365;

            int daysLate = (i == 0) ? 60 : 5;
            double amount = 4500.0 / 10;
            TransactionType type = (i % 2 == 0) ? TransactionType.CREDIT_CARD : TransactionType.AUTO_LOAN;

            engine.addTransaction(ssn, createRecord(amount, daysAgo, daysLate, type));
        }

        engine.evaluateProfile(ssn);
        System.out.println("Scenario 3 Evaluated.\n");
    }

    // Scenario 4 – Poor history: $15,000 limit, $12,000 used, 6/12 on-time, 2-yr age, 1 type, 5 recent inquiries.
    private static void runScenarioFour(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 4: Poor Credit History with Multiple Late Payments");
        String ssn = "444-55-6666";
        User user = new User(ssn, "Maria Garcia", "Madrid, Spain", "maria@example.com", 15000.0);
        engine.registerUser(user);

        for (int i = 0; i < 12; i++) {
            int daysAgo = 2 * 365;
            if (i < 5)
                daysAgo = 180;

            int daysLate = (i < 6) ? 90 : 5;
            double amount = 12000.0 / 12;

            engine.addTransaction(ssn, createRecord(amount, daysAgo, daysLate, TransactionType.CREDIT_CARD));
        }

        engine.evaluateProfile(ssn);
        System.out.println("Scenario 4 Evaluated.\n");
    }

    // Scenario 5 – New user: $1,000 limit, $100 used, 1 on-time payment, minimal history.
    private static void runScenarioFive(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 5: New User with No Credit History");
        String ssn = "555-66-7777";
        User user = new User(ssn, "David Chen", "Tokyo, Japan", "david@example.com", 1000.0);
        engine.registerUser(user);

        engine.addTransaction(ssn, createRecord(100.0, 36, 2, TransactionType.CREDIT_CARD));

        engine.evaluateProfile(ssn);
        System.out.println("Scenario 5 Evaluated.\n");
    }

    private static void demonstrateUserManagement(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING USER MANAGEMENT & TRANSACTION EDITING DEMO");
        String ssn = "111-22-3333";

        System.out.println("Editing User Profile for SSN: " + ssn);
        engine.editUser(ssn, "Jefferson S. Cando", "Guayaquil, Ecuador", "jeff.cando@example.com");

        System.out.println("Adding temporary transaction to be deleted...");
        CreditHistoryRecord tempRecord = createRecord(500.0, 10, 0, TransactionType.LOAN);
        engine.addTransaction(ssn, tempRecord);

        System.out.println("Deleting transaction: " + tempRecord.getTransactionId());
        engine.deleteTransaction(ssn, tempRecord.getTransactionId());

        System.out.println("Deleting User Profile for SSN: 555-66-7777");
        engine.deleteUser("555-66-7777");
    }

    private static CreditHistoryRecord createRecord(double amount, int daysAgoDue, int daysLate, TransactionType type) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgoDue);
        Date dueDate = cal.getTime();

        cal.add(Calendar.DAY_OF_YEAR, daysLate);
        Date settlementDate = cal.getTime();

        return new CreditHistoryRecord(null, dueDate, settlementDate, type, amount, TransactionStatus.PAID);
    }
}