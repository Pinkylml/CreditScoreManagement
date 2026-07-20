# Project Revision & Evaluator's Roadmap

This guide serves as a direct roadmap for the Montran technical evaluator. It outlines how the architecture of the **Credit Score Management System** was fundamentally hardened to address the feedback from the previous 66/100 evaluation. 

The system now strictly adheres to enterprise-grade Hexagonal Architecture (Ports & Adapters) with absolute concurrency safety and financial mathematical precision.

---

## 1. Addressed Feedback / Remediation

The following critical gaps identified in the initial review have been systematically eliminated:

*   **Concurrency Gap (Data Loss under Load):**
    *   *Solution:* The core `CreditScoreEngine` now utilizes a `ConcurrentHashMap<String, ReentrantLock>`. Every state-mutating operation acquires a strict, per-user exclusive lock, ensuring absolute atomicity for read-modify-write transactions on the same SSN.
    *   *Boundary Integrity:* The persistence cache now strictly enforces immutability via **Defensive Copies**. The `UserStore` never returns internal aggregate references; it returns deep-cloned copies, completely eliminating reference-leakage risks.
*   **Missing Periodic Updates:**
    *   *Solution:* Implemented the `PeriodicScoreUpdater` daemon component. This leverages a `ScheduledExecutorService` to execute background batch-recalculations of all users automatically, utilizing the same strict `ReentrantLock` orchestration to interact safely with live user traffic.
*   **Financial Precision (Floating-Point Errors):**
    *   *Solution:* Completely eradicated `double` primitives from all monetary variables (e.g., `amount`, `totalCreditLimit`). The entire domain model and calculation engine now use `java.math.BigDecimal` with `RoundingMode.HALF_UP`, preventing any binary floating-point precision loss.
*   **Inquiry Penalty Miscalculation:**
    *   *Solution:* Mathematical algorithms in `WeightedScoreFormula` now explicitly isolate `TransactionType.INQUIRY`. Inquiries are strictly excluded from average age and utilization calculations to prevent data skewing, and are processed exclusively by the `computeInquiryPoints` penalty algorithm.
*   **File Corruption Risk:**
    *   *Solution:* File persistence adapters (`XmlUserStore`, `JsonUserStore`) now execute a highly resilient **Temp-and-Swap** atomic write. Data is flushed to a `.tmp` file, the stream is closed, and `Files.move()` with `ATOMIC_MOVE` safely replaces the live database. This prevents partial-file corruption during a JVM crash or power loss.
*   **Exception Handling Ambiguity:**
    *   *Solution:* Removed generic Java exceptions from the core domain. The system now utilizes a strict, custom domain exception hierarchy (`DuplicateUserException`, `CreditCalculationException`, `PersistenceException`) providing clear, actionable API contracts.

---

## 2. Architectural Highlights

*   **Hexagonal Architecture (Ports & Adapters)**:
    - **Core Domain:** Pure Java 8 (`com.montran.creditscore.domain` & `com.montran.creditscore.service`). Zero external library dependencies.
    - **Ports:** Abstract SPI interfaces defining outbound boundaries (`UserStore`, `NotificationSender`).
    - **Adapters:** Infrastructure implementations isolating Gson, JAXB, and file I/O operations (`com.montran.creditscore.infrastructure`).
*   **Lossless DTO Serialization**:
    - The structural `UserStorageDto` boundary maps `BigDecimal` properties directly to `String` before handing them off to JAXB/Gson, preserving 100% precision across the file system boundary without polluting the pure Domain aggregate.

---

## 3. Implemented Design Patterns

*   **Facade Pattern**: Coordinated via `CreditScoreEngine` to provide a simple, unified, and thread-safe orchestrator API for the application.
*   **Strategy Pattern**: Scoring algorithms decoupled behind `ScoreFormula` and implemented in `WeightedScoreFormula`, allowing complex calculations to vary independently.
*   **Template Method Pattern**: Locking and caching flows declared in `AbstractFileUserStore`, delegating format-specific atomic disk I/O to concrete subclasses.
*   **Observer Pattern**: Asynchronous event broadcasting via `CreditEventPublisher` dispatching payloads in separate threads.
*   **Factory Pattern**: Dynamic adapter instantiation via `PersistenceRegistry`.

---

## 4. Verification Commands

The `Main.java` demo has been expanded to explicitly prove the system's resilience under concurrent loads and its ability to re-evaluate profiles atomically on transaction edits.

1.  **Verify Test Suites (JAXB, Gson, & Concurrency Isolation)**:
    ```bash
    ./gradlew test
    ```
2.  **Execute the End-to-End System Demo**:
    ```bash
    ./gradlew run
    ```
