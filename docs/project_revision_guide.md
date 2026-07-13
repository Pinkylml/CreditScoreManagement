# Project Revision & Evaluation Guide

This guide is designed for the reviewer to quickly evaluate and verify the key requirements, architectural design, and functionality of the **Credit Score Management System**.

---

## 1. Architectural Highlights

The system is developed using **Hexagonal Architecture (Ports & Adapters)**. This guarantees that all business rules are entirely isolated from infrastructure frameworks (such as JAXB/Gson serialization) and storage formats:

* **Inward Dependencies**:
  - **Core Domain & Services** (`com.montran.creditscore.domain` & `com.montran.creditscore.service`): No external libraries. Uses only standard Java 8 features.
  - **Ports** (`com.montran.creditscore.domain.port`): Abstract SPI interfaces defining outbound gates.
  - **Adapters** (`com.montran.creditscore.infrastructure`): Implements JAXB and Gson serializers, file operations, configurations, and locks.
* **Separation of Entities and DTOs**:
  - Flat serialization constructs ([UserStorageDto](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/dto/UserStorageDto.java)) encapsulate infrastructure tags (JAXB `@XmlElement`, Gson parsing), keeping domain models ([User](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/User.java)) completely clean.

---

## 2. Implemented Design Patterns

* **Facade Pattern**: Coordinated via [CreditScoreEngine.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/CreditScoreEngine.java) to provide a simple, unified orchestrator API for registering profiles, appending records, running evaluations, and firing notifications.
* **Strategy Pattern**: Scoring formulas are decoupled behind [ScoreFormula.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/calculation/ScoreFormula.java) and implemented in [WeightedScoreFormula.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/calculation/WeightedScoreFormula.java), allowing algorithm swaps at runtime.
* **Template Method Pattern**: Base serialization structures (caching, locking, DTO mapping) are declared inside [AbstractFileUserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/AbstractFileUserStore.java), delegating writing/reading mechanisms to adapters ([XmlUserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/XmlUserStore.java) and [JsonUserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/JsonUserStore.java)).
* **Observer Pattern**: Event broadcasting implemented in [CreditEventPublisher.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/CreditEventPublisher.java) to dispatch asynchronous notifications to listeners ([EmailNotificationListener](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/EmailNotificationListener.java) and [SmsNotificationListener](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/SmsNotificationListener.java)).
* **Factory Pattern**: Registry resolved in [PersistenceRegistry.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/PersistenceRegistry.java) to load dynamic storage formats.

---

## 3. Concurrency & Thread-Safety Measures

* **Optimistic Locks (`StampedLock`)**: Read actions validate state optimistically to bypass blocking, acquiring pessimistic read-locks only if a write transaction is detected concurrently.
* **Asynchronous Notifications**: Observers are dispatched concurrently inside separate threads using `CompletableFuture.runAsync()`.
* **Defensive Date Copies**: Mutable `java.util.Date` instances are cloned defensively in `CreditHistoryRecord` constructors and getters to prevent thread interference.
* **Unmodifiable Collections**: The aggregate list `creditHistory` is wrapped inside `Collections.unmodifiableList()` before returning.
* **Thread-Safe Map**: Caching uses `ConcurrentHashMap`.

---

## 4. Capped Mathematical Scoring Formulas

Factor ratings are strictly capped at their configured maximums inside [WeightedScoreFormula.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/calculation/WeightedScoreFormula.java):
* **Credit Utilization**: `Math.min(ratio * maxWeight, maxWeight)` -> Max 30 pts.
* **Payment History**: `Math.min((onTime / total) * maxWeight, maxWeight)` -> Max 35 pts.
* **Credit Age**: `Math.min((age / 10.0) * maxWeight, maxWeight)` -> Max 15 pts.
* **Types of Credit**: `Math.min(types * 2.0, maxWeight)` -> Max 10 pts.
* **Recent Inquiries**: `Math.max(inquiries * -2.0, -maxPenalty)` -> Bounded in `[-10, 0]`.
* **Final Credit Rating**: Strictly capped in the range **`[0.0, 100.0]`**.

---

## 5. Verification Commands

1. **Verify Test Suites (JAXB & Concurrency Isolation)**:
   ```bash
   ./gradlew test
   ```
2. **Execute Five Spec Scenarios and CRUD Demo**:
   ```bash
   ./gradlew run
   ```
