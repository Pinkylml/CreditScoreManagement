package com.montran.creditscore;

import com.montran.creditscore.domain.model.CreditHistoryRecord;
import com.montran.creditscore.domain.model.TransactionStatus;
import com.montran.creditscore.domain.model.TransactionType;
import com.montran.creditscore.domain.model.User;
import com.montran.creditscore.domain.port.outbound.UserStore;
import com.montran.creditscore.infrastructure.persistence.PersistenceRegistry;
import com.montran.creditscore.service.CreditScoreEngine;
import com.montran.creditscore.service.PeriodicScoreUpdater;
import com.montran.creditscore.service.notification.CreditEventPublisher;
import com.montran.creditscore.service.notification.EmailNotificationListener;
import com.montran.creditscore.service.notification.SmsNotificationListener;
import com.montran.creditscore.infrastructure.config.PropertyWeightLoader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Application entry point and end-to-end demonstration of the Credit Score Management System.
 *
 * <p>Five credit-score scenarios are executed first to validate the formula across a range of
 * user profiles. Two additional demos then prove the correctness of Phase 2 and Phase 3
 * requirements:</p>
 * <ol>
 *   <li><strong>Edit Transaction</strong> – adds a transaction, captures its ID, updates it via
 *       {@link CreditScoreEngine#updateTransactionForUser}, and shows the score recalculated.</li>
 *   <li><strong>Safe Concurrency</strong> – spawns several threads that simultaneously call
 *       {@link CreditScoreEngine#addTransactionToUser} on the <em>same</em> user; the
 *       per-user {@link java.util.concurrent.locks.ReentrantLock} serialises writes so no
 *       transaction is lost and the final count matches the expected value.</li>
 * </ol>
 * <p>Finally, {@link PeriodicScoreUpdater} is started for a brief window to demonstrate the
 * scheduled batch-recalculation requirement.</p>
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        separator("INITIALIZING CREDIT SCORE MANAGEMENT SYSTEM");

        UserStore store = PersistenceRegistry.getStore(PropertyWeightLoader.loadStorageType());

        CreditEventPublisher publisher = new CreditEventPublisher();
        publisher.subscribe(new EmailNotificationListener());
        publisher.subscribe(new SmsNotificationListener());

        CreditScoreEngine engine = new CreditScoreEngine(store, publisher);

        // ── Five exam scenarios ──────────────────────────────────────────────
        runScenarioOne(engine);
        runScenarioTwo(engine);
        runScenarioThree(engine);
        runScenarioFour(engine);
        runScenarioFive(engine);

        // ── Phase 5 supplementary demos ─────────────────────────────────────
        demonstrateEditTransaction(engine);
        demonstrateConcurrencySafety(engine);
        demonstrateUserManagement(engine);

        // ── Phase 3: periodic recalculation ─────────────────────────────────
        separator("STARTING PERIODIC SCORE UPDATER (every 2 s)");
        PeriodicScoreUpdater updater = new PeriodicScoreUpdater(engine);
        updater.start(1, 2, TimeUnit.SECONDS);
        System.out.println("[Main] Waiting 5 seconds for at least 2 periodic recalculations...");
        TimeUnit.SECONDS.sleep(5);
        updater.stop();

        separator("SYSTEM DEMO COMPLETE");
    }

    // =========================================================================
    // Scenario runners
    // =========================================================================

    // Scenario 1 – Excellent history: $10,000 limit, $2,000 used, 20 on-time, 5-yr age, 3 types, 1 inquiry.
    // Utilization: (2000/10000)*30=6 | Payment: (20/20)*35=35 | Age: ~7.5 | Types: 3*2=6 | Inquiries: 1*-2=-2 → ~52.5
    private static void runScenarioOne(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 1: Excellent Credit History");
        String ssn = "111-22-3333";
        engine.registerUser(new User(ssn, "Jefferson Cando", "Quito, Ecuador", "jeff@example.com",
                new BigDecimal("10000.00")));

        for (int i = 0; i < 20; i++) {
            int yearsAgo = (i == 0) ? 1 : 6;
            BigDecimal amount = bd("2000.00").divide(bd("20"), 2, RoundingMode.HALF_UP);
            TransactionType type = (i % 3 == 0) ? TransactionType.CREDIT_CARD
                    : (i % 3 == 1) ? TransactionType.MORTGAGE : TransactionType.AUTO_LOAN;
            engine.addTransactionToUser(ssn, createRecord(amount, yearsAgo * 365, 0, type));
        }
        engine.addTransactionToUser(ssn, createInquiry(180));
        engine.evaluateProfile(ssn);
        System.out.println("Scenario 1 Evaluated.\n");
    }

    // Scenario 2 – Good history with late payments: $20,000 limit, $8,000 used, 13/15 on-time, 8-yr age, 4 types.
    // Utilization: (8000/20000)*30=12 | Payment: (13/15)*35≈30.33 | Age: ~12 | Types: 4*2=8 | Inquiries: 0 → ~62.33
    private static void runScenarioTwo(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 2: Good Credit History with a Few Late Payments");
        String ssn = "222-33-4444";
        engine.registerUser(new User(ssn, "Alice Smith", "New York, USA", "alice@example.com",
                new BigDecimal("20000.00")));

        for (int i = 0; i < 15; i++) {
            int daysLate = (i < 2) ? 45 : 0;
            BigDecimal amount = bd("8000.00").divide(bd("15"), 2, RoundingMode.HALF_UP);
            engine.addTransactionToUser(ssn,
                    createRecord(amount, 8 * 365, daysLate, TransactionType.values()[i % 4]));
        }
        engine.evaluateProfile(ssn);
        System.out.println("Scenario 2 Evaluated.\n");
    }

    // Scenario 3 – High utilization: $5,000 limit, $4,500 used, 9/10 on-time, 3-yr age, 2 types, 3 inquiries.
    // Utilization: (4500/5000)*30=27 | Payment: (9/10)*35=31.5 | Age: ~4.5 | Types: 2*2=4 | Inquiries: 3*-2=-6 → ~61
    private static void runScenarioThree(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 3: Moderate Credit History with High Utilization");
        String ssn = "333-44-5555";
        engine.registerUser(new User(ssn, "Robert Johnson", "London, UK", "robert@example.com",
                new BigDecimal("5000.00")));

        for (int i = 0; i < 10; i++) {
            int daysLate = (i == 0) ? 60 : 0;
            BigDecimal amount = bd("4500.00").divide(bd("10"), 2, RoundingMode.HALF_UP);
            TransactionType type = (i % 2 == 0) ? TransactionType.CREDIT_CARD : TransactionType.AUTO_LOAN;
            engine.addTransactionToUser(ssn, createRecord(amount, 3 * 365, daysLate, type));
        }
        for (int i = 0; i < 3; i++) {
            engine.addTransactionToUser(ssn, createInquiry(365));
        }
        engine.evaluateProfile(ssn);
        System.out.println("Scenario 3 Evaluated.\n");
    }

    // Scenario 4 – Poor history: $15,000 limit, $12,000 used, 6/12 on-time, 2-yr age, 1 type, 5 inquiries.
    // Utilization: (12000/15000)*30=24 | Payment: (6/12)*35=17.5 | Age: ~3 | Types: 1*2=2 | Inquiries: 5*-2=-10 → ~36.5
    private static void runScenarioFour(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 4: Poor Credit History with Multiple Late Payments");
        String ssn = "444-55-6666";
        engine.registerUser(new User(ssn, "Maria Garcia", "Madrid, Spain", "maria@example.com",
                new BigDecimal("15000.00")));

        for (int i = 0; i < 12; i++) {
            int daysLate = (i < 6) ? 90 : 0;
            BigDecimal amount = bd("12000.00").divide(bd("12"), 2, RoundingMode.HALF_UP);
            engine.addTransactionToUser(ssn,
                    createRecord(amount, 2 * 365, daysLate, TransactionType.CREDIT_CARD));
        }
        for (int i = 0; i < 5; i++) {
            engine.addTransactionToUser(ssn, createInquiry(180));
        }
        engine.evaluateProfile(ssn);
        System.out.println("Scenario 4 Evaluated.\n");
    }

    // Scenario 5 – New user: $1,000 limit, $100 used, 1 on-time, minimal history, 1 inquiry.
    // Utilization: (100/1000)*30=3 | Payment: (1/1)*35=35 | Age: ~0.15 | Types: 1*2=2 | Inquiries: 1*-2=-2 → ~38.15
    private static void runScenarioFive(CreditScoreEngine engine) {
        System.out.println(">>> EXECUTING SCENARIO 5: New User with No Credit History");
        String ssn = "555-66-7777";
        engine.registerUser(new User(ssn, "David Chen", "Tokyo, Japan", "david@example.com",
                new BigDecimal("1000.00")));

        engine.addTransactionToUser(ssn, createRecord(bd("100.00"), 36, 0, TransactionType.CREDIT_CARD));
        engine.addTransactionToUser(ssn, createInquiry(36));
        engine.evaluateProfile(ssn);
        System.out.println("Scenario 5 Evaluated.\n");
    }

    // =========================================================================
    // Phase 5 supplementary demos
    // =========================================================================

    /**
     * Proves that {@link CreditScoreEngine#updateTransactionForUser} works correctly:
     * a transaction is added, then its amount is changed, and the recalculated score
     * is printed to confirm automatic re-evaluation after the edit.
     *
     * <p>Uses SSN {@code 111-22-3333} (Scenario 1 user) which already has a full credit
     * history, so score changes are meaningful.</p>
     */
    private static void demonstrateEditTransaction(CreditScoreEngine engine) {
        separator("DEMO: EDIT TRANSACTION (atomic update + automatic re-evaluation)");
        String ssn = "111-22-3333";

        // Add a new loan record and capture its auto-generated ID.
        CreditHistoryRecord originalRecord = createRecord(bd("3000.00"), 90, 0, TransactionType.LOAN);
        engine.addTransactionToUser(ssn, originalRecord);
        String txId = originalRecord.getTransactionId();

        // Read the score before the edit.
        Optional<User> before = engine.findUser(ssn);
        double scoreBefore = before.map(User::getCreditScore).orElse(0.0);
        System.out.printf("[EditTx] Added LOAN $3,000 (id=%s). Score after add: %.2f%n", txId, scoreBefore);

        // Replace the record with a much larger amount to trigger a score shift.
        CreditHistoryRecord updatedRecord = new CreditHistoryRecord(
                txId,
                originalRecord.getDueDate(),
                originalRecord.getSettlementDate(),
                TransactionType.LOAN,
                bd("9000.00"),
                TransactionStatus.PAID);
        engine.updateTransactionForUser(ssn, txId, updatedRecord);

        // Read the score after the edit.
        Optional<User> after = engine.findUser(ssn);
        double scoreAfter = after.map(User::getCreditScore).orElse(0.0);
        System.out.printf("[EditTx] Updated LOAN to $9,000. Score after edit: %.2f%n", scoreAfter);
        System.out.printf("[EditTx] Score delta: %+.2f → recalculation confirmed.%n%n", scoreAfter - scoreBefore);
    }

    /**
     * Proves that the per-user {@link java.util.concurrent.locks.ReentrantLock} prevents data
     * loss under concurrent writes to the same user.
     *
     * <p>Three threads simultaneously call {@link CreditScoreEngine#addTransactionToUser} for
     * SSN {@code 444-55-6666} (Scenario 4 user). A {@link CountDownLatch} synchronises
     * thread start so contention is as tight as possible. After all threads finish, the final
     * transaction count is verified against the expected value.</p>
     */
    private static void demonstrateConcurrencySafety(CreditScoreEngine engine)
            throws InterruptedException {
        separator("DEMO: CONCURRENCY SAFETY (3 simultaneous addTransactionToUser calls)");
        String ssn = "444-55-6666";

        // Snapshot the current transaction count before the concurrent writes.
        int countBefore = engine.findUser(ssn)
                .map(u -> u.getCreditHistory().size())
                .orElse(0);
        System.out.printf("[Concurrency] Starting transaction count: %d%n", countBefore);

        int threadCount = 3;
        CountDownLatch startGate = new CountDownLatch(1);  // all threads wait here
        CountDownLatch doneLatch = new CountDownLatch(threadCount); // main waits here
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<String> errors = new java.util.concurrent.CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    startGate.await(); // wait until all threads are ready
                    CreditHistoryRecord record = createRecord(
                            bd("500.00"), 30 + idx, 0, TransactionType.CREDIT_CARD);
                    engine.addTransactionToUser(ssn, record);
                    System.out.printf("[Concurrency] Thread-%d added transaction %s%n",
                            idx, record.getTransactionId());
                } catch (Exception e) {
                    errors.add("Thread-" + idx + ": " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // release all threads simultaneously
        doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        int countAfter = engine.findUser(ssn)
                .map(u -> u.getCreditHistory().size())
                .orElse(0);
        int expected = countBefore + threadCount;

        System.out.printf("[Concurrency] Expected count: %d | Actual count: %d | Errors: %d%n",
                expected, countAfter, errors.size());
        if (countAfter == expected && errors.isEmpty()) {
            System.out.println("[Concurrency] ✓ All transactions persisted – no race conditions detected.\n");
        } else {
            System.out.println("[Concurrency] ✗ DATA LOSS OR ERRORS DETECTED – locking may be broken.\n");
            errors.forEach(System.err::println);
        }
    }

    /** Demonstrates profile editing and transaction deletion on the Scenario 1 user. */
    private static void demonstrateUserManagement(CreditScoreEngine engine) {
        separator("DEMO: USER MANAGEMENT & TRANSACTION DELETION");
        String ssn = "111-22-3333";

        engine.editUser(ssn, "Jefferson S. Cando", "Guayaquil, Ecuador", "jeff.cando@example.com");
        System.out.println("[UserMgmt] Profile updated for SSN: " + ssn);

        CreditHistoryRecord tempRecord = createRecord(bd("250.00"), 5, 0, TransactionType.STUDENT_LOAN);
        engine.addTransactionToUser(ssn, tempRecord);
        System.out.println("[UserMgmt] Temporary transaction added: " + tempRecord.getTransactionId());

        engine.deleteTransaction(ssn, tempRecord.getTransactionId());
        System.out.println("[UserMgmt] Temporary transaction deleted: " + tempRecord.getTransactionId());

        engine.deleteUser("555-66-7777");
        System.out.println("[UserMgmt] User 555-66-7777 removed from the system.\n");
    }

    // =========================================================================
    // Factory helpers
    // =========================================================================

    private static CreditHistoryRecord createRecord(BigDecimal amount, int daysAgoDue,
            int daysLate, TransactionType type) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgoDue);
        Date dueDate = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, daysLate);
        return new CreditHistoryRecord(null, dueDate, cal.getTime(), type, amount, TransactionStatus.PAID);
    }

    /**
     * Creates a hard-inquiry record. {@code amount} is always {@link BigDecimal#ZERO} because
     * inquiries carry no monetary obligation.
     *
     * @param daysAgo How many days ago the inquiry was recorded.
     */
    private static CreditHistoryRecord createInquiry(int daysAgo) {
        return createRecord(BigDecimal.ZERO, daysAgo, 0, TransactionType.INQUIRY);
    }

    /** Shorthand for {@code new BigDecimal(value)} to keep scenario code concise. */
    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    /** Prints a formatted section separator with a centred title. */
    private static void separator(String title) {
        System.out.println("\n===================================================");
        System.out.printf("   %-47s%n", title);
        System.out.println("===================================================");
    }
}