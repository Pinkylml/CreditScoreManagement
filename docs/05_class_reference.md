# 05. Codebase Class Reference Guide

This document provides a detailed class-by-class guide of the **CreditScoreManagement** system, complete with code declarations and descriptions of their operational roles to facilitate developer onboarding.

---

## 1. Domain Layer (`com.montran.creditscore.domain.model`)

### [User.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/User.java)
Acts as the **Aggregate Root** representing a customer profile. Enforces constraints (invariants) and provides thread-safe access to transactional history.
```java
public class User {
    private final String ssn;
    private String name;
    private String address;
    private String email;
    private double creditScore;
    private BigDecimal totalCreditLimit;
    private RiskLevel riskLevel;
    private final List<CreditHistoryRecord> creditHistory;

    public User(String ssn, String name, String address, String email, BigDecimal totalCreditLimit);
    public User(User source); // Defensive copy constructor
    public void addCreditRecord(CreditHistoryRecord record);
    public boolean removeCreditRecord(String transactionId);
    public boolean updateCreditRecord(String transactionId, CreditHistoryRecord updatedRecord);
    public void updateProfile(String newName, String newAddress, String newEmail);
}
```

### [CreditHistoryRecord.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/CreditHistoryRecord.java)
An **Immutable Value Object** representing a single credit transaction log.
```java
public final class CreditHistoryRecord {
    private final String transactionId;
    private final Date dueDate;
    private final Date settlementDate;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final TransactionStatus status;

    public CreditHistoryRecord(CreditHistoryRecord source); // Defensive copy constructor
    public boolean isDefaulted();
}
```

### [ScoreConfiguration.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/ScoreConfiguration.java)
An **Immutable Configuration Object** mapping parameters read from properties.

### Domain Enumerations
* **[RiskLevel.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/RiskLevel.java)**: `LOW`, `MEDIUM`, `HIGH`
* **[TransactionStatus.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/TransactionStatus.java)**: `PAID`, `DEFAULTED`
* **[TransactionType.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/TransactionType.java)**: `CREDIT_CARD`, `MORTGAGE`, `AUTO_LOAN`, `STUDENT_LOAN`, `LOAN`, `INQUIRY`

---

## 2. Domain Exceptions (`com.montran.creditscore.domain.exception`)

The system utilizes a strict domain exception hierarchy to guarantee API contracts:
* **[DuplicateUserException.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/exception/DuplicateUserException.java)**: Thrown by `CreditScoreEngine` when attempting to register an SSN that already exists.
* **[CreditCalculationException.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/exception/CreditCalculationException.java)**: Thrown by `WeightedScoreFormula` to halt calculations if required financial math cannot be completed safely (e.g., divide by zero on missing credit limit).
* **[PersistenceException.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/exception/PersistenceException.java)**: Thrown by outbound adapters to encapsulate raw I/O or XML/JSON marshalling failures into a domain-understood fault.
* **[UserNotFoundException.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/exception/UserNotFoundException.java)**: Thrown on attempted operations against a non-existent SSN.

---

## 3. Boundary Ports (`com.montran.creditscore.domain.port.outbound`)

### [UserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/port/outbound/UserStore.java)
Outbound Port interface defining the SPI database contract.
```java
public interface UserStore {
    void save(User user);
    Optional<User> findBySsn(String ssn);
    List<User> findAll();
    boolean deleteBySsn(String ssn);
}
```

### [NotificationSender.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/port/outbound/NotificationSender.java)
Outbound Port defining the event-dispatch mechanism.

---

## 4. Orchestration Services (`com.montran.creditscore.service`)

### [CreditScoreEngine.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/CreditScoreEngine.java)
A **Facade orchestrator** providing strictly atomic endpoints. Maintains a `ConcurrentHashMap<String, ReentrantLock>` to guarantee serialized access per SSN.
```java
public class CreditScoreEngine {
    public void registerUser(User user);
    // Atomic Transaction Management:
    public void addTransactionToUser(String ssn, CreditHistoryRecord record);
    public void updateTransactionForUser(String ssn, String transactionId, CreditHistoryRecord updatedRecord);
    public void deleteTransaction(String ssn, String transactionId);
    // Profile Management:
    public void evaluateProfile(String ssn);
    public void recalculateAllUsers(); // Executes safe batch processing
    public void editUser(String ssn, String newName, String newAddress, String newEmail);
    public void deleteUser(String ssn);
}
```

### [PeriodicScoreUpdater.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/PeriodicScoreUpdater.java)
A dedicated daemon component hosting a `ScheduledExecutorService`. Responsible for triggering `engine.recalculateAllUsers()` on a background schedule to satisfy periodic batch requirements safely.

---

## 5. Mathematical Scoring & Classification (`com.montran.creditscore.service.calculation` & `com.montran.creditscore.service.risk`)

### [ScoreFormula.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/calculation/ScoreFormula.java)
Strategy Interface for calculation algorithms.

### [WeightedScoreFormula.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/calculation/WeightedScoreFormula.java)
Concrete strategy executing mathematically precise scoring calculations using `BigDecimal`.
```java
public class WeightedScoreFormula implements ScoreFormula {
    @Override
    public double calculate(User user, ScoreConfiguration config);
    // Component algorithms:
    private double computeUtilizationPoints(User user, List<CreditHistoryRecord> history, int maxWeight);
    private double computePaymentHistoryPoints(List<CreditHistoryRecord> history, int maxWeight, int graceDays);
    private double computeCreditAgePoints(List<CreditHistoryRecord> history, int maxWeight);
    private double computeCreditTypesPoints(List<CreditHistoryRecord> history, int maxWeight);
    private double computeInquiryPoints(List<CreditHistoryRecord> history, int maxPenalty);
}
```

### [RiskClassifier.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/risk/RiskClassifier.java)
Resolves risk levels based on settings thresholds.

---

## 6. Event Notification Observers (`com.montran.creditscore.service.notification`)

### [CreditEventPublisher.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/CreditEventPublisher.java)
Subject broadcasting state changes asynchronously inside separate threads using `CompletableFuture.runAsync()`.
* **[CreditEventListener.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/CreditEventListener.java)**: Interface for listeners.
* **[EmailNotificationListener.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/EmailNotificationListener.java)**: Observer printing email simulations.
* **[SmsNotificationListener.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/SmsNotificationListener.java)**: Observer printing SMS simulations.

---

## 7. Infrastructure Adapters (`com.montran.creditscore.infrastructure.persistence`)

### [AbstractFileUserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/AbstractFileUserStore.java)
Abstract template class providing memory caching, defensive copy translation, `StampedLock` isolation, and domain mapping logic.
* **[XmlUserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/XmlUserStore.java)**: Marshals DTO lists to file using JAXB formatting and `ATOMIC_MOVE` temp-and-swap resilience.
* **[JsonUserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/JsonUserStore.java)**: Marshals DTO lists using Gson and `ATOMIC_MOVE` temp-and-swap resilience.
* **[PersistenceRegistry.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/PersistenceRegistry.java)**: Registry Factory returning active adapters.

---

## 8. Serialization DTOs & Configuration Loader

* **[UserStorageDto.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/dto/UserStorageDto.java)**: XML/JSON transfer data nodes (maps `BigDecimal` to `String` for lossless save).
* **[CreditHistoryStorageDto.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/dto/CreditHistoryStorageDto.java)**: Transaction transfer data nodes.
* **[SystemContainerDto.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/dto/SystemContainerDto.java)**: Root container class decorated with `@XmlRootElement` for XML serialization.
* **[PropertyWeightLoader.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/config/PropertyWeightLoader.java)**: Loads mathematical weights and target `StorageType` dynamically.
