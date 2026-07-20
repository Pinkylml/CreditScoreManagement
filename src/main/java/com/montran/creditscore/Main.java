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

import java.math.BigDecimal;
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
    // Utilization: (2000/10000)*30 = 6  | Payment: (20/20)*35 = 35 | Age: ~7.5 | Types: 3*2=6 | Inquiries: 1*-2=-2 → ~52.5
    private static void runScenarioOne(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 1: Excellent Credit History");
        String ssn = "111-22-3333";
        User user = new User(ssn, "Jefferson Cando", "Quito, Ecuador", "jeff@example.com",
                new BigDecimal("10000.00"));
        engine.registerUser(user);

        // 20 on-time payments distributing $2,000 across 3 credit types.
        // i==0 → 1 year ago (within inquiry window); rest → 6 years ago (outside window).
        for (int i = 0; i < 20; i++) {
            int yearsAgo = (i == 0) ? 1 : 6;
            BigDecimal amount = new BigDecimal("2000.00").divide(new BigDecimal("20"), 2, java.math.RoundingMode.HALF_UP);
            TransactionType type = (i % 3 == 0) ? TransactionType.CREDIT_CARD
                    : (i % 3 == 1) ? TransactionType.MORTGAGE : TransactionType.AUTO_LOAN;

            // daysLate=0: settled exactly on the due date → on time
            engine.addTransaction(ssn, createRecord(amount, yearsAgo * 365, 0, type));
        }

        // 1 explicit hard inquiry within the last 2 years → penalty -2
        engine.addTransaction(ssn, createInquiry(180));

        engine.evaluateProfile(ssn);
        System.out.println("Scenario 1 Evaluated.\n");
    }

    // Scenario 2 – Good history with late payments: $20,000 limit, $8,000 used, 13/15 on-time, 8-yr age, 4 types, 0 recent inquiries.
    // Utilization: (8000/20000)*30=12 | Payment: (13/15)*35≈30.33 | Age: ~12 | Types: 4*2=8 | Inquiries: 0 → ~62.33
    private static void runScenarioTwo(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 2: Good Credit History with a Few Late Payments");
        String ssn = "222-33-4444";
        User user = new User(ssn, "Alice Smith", "New York, USA", "alice@example.com",
                new BigDecimal("20000.00"));
        engine.registerUser(user);

        for (int i = 0; i < 15; i++) {
            int yearsAgo = 8;
            // First 2 payments are late (settled 45 days after due), remaining 13 are on time (daysLate=0)
            int daysLate = (i < 2) ? 45 : 0;
            BigDecimal amount = new BigDecimal("8000.00").divide(new BigDecimal("15"), 2, java.math.RoundingMode.HALF_UP);
            TransactionType type = TransactionType.values()[i % 4];

            engine.addTransaction(ssn, createRecord(amount, yearsAgo * 365, daysLate, type));
        }

        // No INQUIRY records → 0 inquiries penalty
        engine.evaluateProfile(ssn);
        System.out.println("Scenario 2 Evaluated.\n");
    }

    // Scenario 3 – High utilization: $5,000 limit, $4,500 used, 9/10 on-time, 3-yr age, 2 types, 3 recent inquiries.
    // Utilization: (4500/5000)*30=27 | Payment: (9/10)*35=31.5 | Age: ~4.5 | Types: 2*2=4 | Inquiries: 3*-2=-6 → ~61
    private static void runScenarioThree(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 3: Moderate Credit History with High Utilization");
        String ssn = "333-44-5555";
        User user = new User(ssn, "Robert Johnson", "London, UK", "robert@example.com",
                new BigDecimal("5000.00"));
        engine.registerUser(user);

        for (int i = 0; i < 10; i++) {
            int daysAgo = 3 * 365;

            // First payment is late (60 days after due); remaining 9 are on time (daysLate=0)
            int daysLate = (i == 0) ? 60 : 0;
            BigDecimal amount = new BigDecimal("4500.00").divide(new BigDecimal("10"), 2, java.math.RoundingMode.HALF_UP);
            TransactionType type = (i % 2 == 0) ? TransactionType.CREDIT_CARD : TransactionType.AUTO_LOAN;

            engine.addTransaction(ssn, createRecord(amount, daysAgo, daysLate, type));
        }

        // 3 explicit hard inquiries within the last 2 years (1 year ago) → penalty 3*-2=-6
        for (int i = 0; i < 3; i++) {
            engine.addTransaction(ssn, createInquiry(365));
        }

        engine.evaluateProfile(ssn);
        System.out.println("Scenario 3 Evaluated.\n");
    }

    // Scenario 4 – Poor history: $15,000 limit, $12,000 used, 6/12 on-time, 2-yr age, 1 type, 5 recent inquiries.
    // Utilization: (12000/15000)*30=24 | Payment: (6/12)*35=17.5 | Age: ~3 | Types: 1*2=2 | Inquiries: 5*-2=-10 → ~36.5
    private static void runScenarioFour(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 4: Poor Credit History with Multiple Late Payments");
        String ssn = "444-55-6666";
        User user = new User(ssn, "Maria Garcia", "Madrid, Spain", "maria@example.com",
                new BigDecimal("15000.00"));
        engine.registerUser(user);

        for (int i = 0; i < 12; i++) {
            int daysAgo = 2 * 365;

            // First 6 payments are late (90 days after due); last 6 are on time (daysLate=0)
            int daysLate = (i < 6) ? 90 : 0;
            BigDecimal amount = new BigDecimal("12000.00").divide(new BigDecimal("12"), 2, java.math.RoundingMode.HALF_UP);

            engine.addTransaction(ssn, createRecord(amount, daysAgo, daysLate, TransactionType.CREDIT_CARD));
        }

        // 5 explicit hard inquiries within the last 2 years (6 months ago) → penalty 5*-2=-10
        for (int i = 0; i < 5; i++) {
            engine.addTransaction(ssn, createInquiry(180));
        }

        engine.evaluateProfile(ssn);
        System.out.println("Scenario 4 Evaluated.\n");
    }

    // Scenario 5 – New user: $1,000 limit, $100 used, 1 on-time payment, minimal history, 1 recent inquiry.
    // Utilization: (100/1000)*30=3 | Payment: (1/1)*35=35 | Age: ~0.15 | Types: 1*2=2 | Inquiries: 1*-2=-2 → ~38.15
    private static void runScenarioFive(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 5: New User with No Credit History");
        String ssn = "555-66-7777";
        User user = new User(ssn, "David Chen", "Tokyo, Japan", "david@example.com",
                new BigDecimal("1000.00"));
        engine.registerUser(user);

        // 1 on-time payment, due 36 days ago → inside the 2-year inquiry window
        engine.addTransaction(ssn, createRecord(new BigDecimal("100.00"), 36, 0, TransactionType.CREDIT_CARD));

        // 1 explicit hard inquiry within the last 2 years → penalty -2
        engine.addTransaction(ssn, createInquiry(36));

        engine.evaluateProfile(ssn);
        System.out.println("Scenario 5 Evaluated.\n");
    }

    private static void demonstrateUserManagement(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING USER MANAGEMENT & TRANSACTION EDITING DEMO");
        String ssn = "111-22-3333";

        System.out.println("Editing User Profile for SSN: " + ssn);
        engine.editUser(ssn, "Jefferson S. Cando", "Guayaquil, Ecuador", "jeff.cando@example.com");

        System.out.println("Adding temporary transaction to be deleted...");
        CreditHistoryRecord tempRecord = createRecord(new BigDecimal("500.00"), 10, 0, TransactionType.LOAN);
        engine.addTransaction(ssn, tempRecord);

        System.out.println("Deleting transaction: " + tempRecord.getTransactionId());
        engine.deleteTransaction(ssn, tempRecord.getTransactionId());

        System.out.println("Deleting User Profile for SSN: 555-66-7777");
        engine.deleteUser("555-66-7777");
    }

    private static CreditHistoryRecord createRecord(BigDecimal amount, int daysAgoDue, int daysLate,
            TransactionType type) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgoDue);
        Date dueDate = cal.getTime();

        cal.add(Calendar.DAY_OF_YEAR, daysLate);
        Date settlementDate = cal.getTime();

        return new CreditHistoryRecord(null, dueDate, settlementDate, type, amount, TransactionStatus.PAID);
    }

    /**
     * Creates a hard-inquiry record with a zero amount (inquiries carry no monetary value).
     * The due date is set to {@code daysAgo} days in the past; settlement is on the same day.
     *
     * @param daysAgo how many days ago the inquiry occurred
     */
    private static CreditHistoryRecord createInquiry(int daysAgo) {
        return createRecord(BigDecimal.ZERO, daysAgo, 0, TransactionType.INQUIRY);
    }
}