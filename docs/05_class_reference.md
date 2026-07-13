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
    private double totalCreditLimit;
    private RiskLevel riskLevel;
    private final List<CreditHistoryRecord> creditHistory;

    public User(String ssn, String name, String address, String email, double totalCreditLimit);
    public void addCreditRecord(CreditHistoryRecord record);
    public boolean removeCreditRecord(String transactionId);
    public boolean updateCreditRecord(String transactionId, CreditHistoryRecord updatedRecord);
    public void updateProfile(String newName, String newAddress, String newEmail);
    // Getters and Setters
}
```

### [CreditHistoryRecord.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/CreditHistoryRecord.java)
An **Immutable Value Object** representing a single credit transaction log. Defensively clones dates to ensure thread-safety.
```java
public final class CreditHistoryRecord {
    private final String transactionId;
    private final Date dueDate;
    private final Date settlementDate;
    private final TransactionType transactionType;
    private final double amount;
    private final TransactionStatus status;

    public CreditHistoryRecord(String transactionId, Date dueDate, Date settlementDate, TransactionType transactionType, double amount, TransactionStatus status);
    public boolean isDefaulted();
    // Getters only (no setters)
}
```

### [ScoreConfiguration.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/ScoreConfiguration.java)
An **Immutable Configuration Object** mapping parameters read from properties.
```java
public final class ScoreConfiguration {
    private final int utilizationWeight;
    private final int paymentHistoryWeight;
    private final int creditAgeWeight;
    private final int creditTypesWeight;
    private final int recentInquiriesWeight;
    private final int latePaymentGraceDays;
    private final double riskThresholdLow;
    private final double riskThresholdMedium;
    // Constructor and Getters
}
```

### Domain Enumerations
* **[RiskLevel.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/RiskLevel.java)**: `LOW`, `MEDIUM`, `HIGH`
* **[TransactionStatus.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/TransactionStatus.java)**: `PAID`, `DEFAULTED`
* **[TransactionType.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/model/TransactionType.java)**: `CREDIT_CARD`, `MORTGAGE`, `AUTO_LOAN`, `STUDENT_LOAN`, `LOAN`

---

## 2. Domain Exceptions (`com.montran.creditscore.domain.exception`)

* **[UserNotFoundException.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/exception/UserNotFoundException.java)**: Thrown when an operations is attempted on a non-existent SSN profile.
* **[InvalidCreditDataException.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/exception/InvalidCreditDataException.java)**: Thrown when credit limits are `<= 0` or transaction amounts are negative.

---

## 3. Boundary Ports (`com.montran.creditscore.domain.port.outbound`)

### [UserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/port/outbound/UserStore.java)
Outbound Port interface defining the SPI database contract:
```java
public interface UserStore {
    void save(User user);
    Optional<User> findBySsn(String ssn);
    List<User> findAll();
    boolean deleteBySsn(String ssn);
}
```

### [NotificationSender.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/domain/port/outbound/NotificationSender.java)
Outbound Port defining the event-dispatch mechanism:
```java
public interface NotificationSender {
    void sendNotification(User user, String message);
}
```

---

## 4. Orchestration Services (`com.montran.creditscore.service`)

### [CreditScoreEngine.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/CreditScoreEngine.java)
A **Facade orchestrator** providing atomic endpoints to manage profiles and evaluate rating metrics:
```java
public class CreditScoreEngine {
    private final UserStore userStore;
    private final NotificationSender notificationSender;
    private final ScoreFormula scoreFormula;

    public CreditScoreEngine(UserStore userStore, NotificationSender notificationSender);
    public void registerUser(User user);
    public void addTransaction(String ssn, CreditHistoryRecord record);
    public void evaluateProfile(String ssn);
    public void deleteUser(String ssn);
    public void editUser(String ssn, String newName, String newAddress, String newEmail);
    public void deleteTransaction(String ssn, String transactionId);
    public void editTransaction(String ssn, String transactionId, CreditHistoryRecord updatedRecord);
}
```

---

## 5. Mathematical Scoring & Classification (`com.montran.creditscore.service.calculation` & `com.montran.creditscore.service.risk`)

### [ScoreFormula.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/calculation/ScoreFormula.java)
Strategy Interface for calculation algorithms:
```java
public interface ScoreFormula {
    double calculate(User user, ScoreConfiguration config);
}
```

### [WeightedScoreFormula.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/calculation/WeightedScoreFormula.java)
Concrete strategy executing calculations across credit components:
```java
public class WeightedScoreFormula implements ScoreFormula {
    @Override
    public double calculate(User user, ScoreConfiguration config);
    private double computeUtilizationPoints(User user, List<CreditHistoryRecord> history, int maxWeight);
    private double computePaymentHistoryPoints(List<CreditHistoryRecord> history, int maxWeight, int graceDays);
    private double computeCreditAgePoints(List<CreditHistoryRecord> history, int maxWeight);
    private double computeCreditTypesPoints(List<CreditHistoryRecord> history, int maxWeight);
    private double computeInquiryPoints(List<CreditHistoryRecord> history, int maxPenalty);
}
```

### [RiskClassifier.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/risk/RiskClassifier.java)
Resolves risk levels based on settings thresholds:
```java
public final class RiskClassifier {
    public static RiskLevel classify(double score, double lowThreshold, double mediumThreshold);
}
```

---

## 6. Event Notification Observers (`com.montran.creditscore.service.notification`)

### [CreditEventPublisher.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/CreditEventPublisher.java)
Subject broadcasting state changes asynchronously inside separate threads:
```java
public class CreditEventPublisher implements NotificationSender {
    private final List<CreditEventListener> listeners = new CopyOnWriteArrayList<>();
    public void subscribe(CreditEventListener listener);
    public void unsubscribe(CreditEventListener listener);
    @Override
    public void sendNotification(User user, String message);
}
```

* **[CreditEventListener.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/CreditEventListener.java)**: Interface for listeners.
* **[EmailNotificationListener.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/EmailNotificationListener.java)**: Observer printing email simulations.
* **[SmsNotificationListener.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/service/notification/SmsNotificationListener.java)**: Observer printing SMS simulations.

---

## 7. Infrastructure Adapters (`com.montran.creditscore.infrastructure.persistence`)

### [AbstractFileUserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/AbstractFileUserStore.java)
Abstract template class providing caching, StampedLock concurrency isolation, and domain mapping wrapper logic:
```java
public abstract class AbstractFileUserStore implements UserStore {
    protected final ConcurrentMap<String, User> cache = new ConcurrentHashMap<>();
    protected final StampedLock lock = new StampedLock();
    private final String filePath;

    public AbstractFileUserStore(String filePath);
    public void loadStateIntoCache();
    public void flushCacheToFile();
    // SPI implementations with locks
    protected abstract List<UserStorageDto> readFromFile() throws Exception;
    protected abstract void writeToFile(List<UserStorageDto> dtos) throws Exception;
}
```

* **[XmlUserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/XmlUserStore.java)**: Marshals DTO lists to file using JAXB formatting.
* **[JsonUserStore.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/JsonUserStore.java)**: Marshals DTO lists using Gson.
* **[PersistenceRegistry.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/PersistenceRegistry.java)**: Registry Factory returning active adapters.

---

## 8. Serialization DTOs & Configuration Loader

* **[UserStorageDto.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/dto/UserStorageDto.java)**: XML/JSON transfer data nodes.
* **[CreditHistoryStorageDto.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/dto/CreditHistoryStorageDto.java)**: Transaction transfer data nodes.
* **[SystemContainerDto.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/persistence/dto/SystemContainerDto.java)**: Root container class decorated with `@XmlRootElement` for XML serialization.
* **[PropertyWeightLoader.java](file:///c:/Users/jcando/projects/Simulacro/CreditScoreManagement/src/main/java/com/montran/creditscore/infrastructure/config/PropertyWeightLoader.java)**: Loads configurations dynamically:
  ```java
  public class PropertyWeightLoader {
      public static ScoreConfiguration loadWeights();
      public static StorageType loadStorageType();
  }
  ```
